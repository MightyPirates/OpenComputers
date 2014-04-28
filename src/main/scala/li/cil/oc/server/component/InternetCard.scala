package li.cil.oc.server.component

import java.io.{IOException, OutputStreamWriter, BufferedWriter, FileNotFoundException}
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.regex.Matcher
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ThreadPoolFactory
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import scala.collection.mutable
import li.cil.oc.api.prefab.AbstractValue

class InternetCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  val romInternet = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/internet"), "internet"))

  protected var owner: Option[Context] = None

  protected val connections = mutable.Map.empty[Int, SocketChannel]

  protected var request: Option[InternetCard.Request] = None

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether HTTP requests can be made (config setting).""")
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(url:string[, postData:string]):boolean -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.""")
  def request(context: Context, args: Arguments): Array[AnyRef] = {
    if (owner.isEmpty || context.node.address != owner.get.node.address) {
      throw new IllegalArgumentException("can only be used by the owning computer")
    }
    val address = args.checkString(0)
    if (!Settings.get.httpEnabled) {
      return result(Unit, "http requests are unavailable")
    }
    val post = if (args.isString(1)) Option(args.checkString(1)) else None
    this.synchronized {
      if (request.isDefined) {
        return result(Unit, "already busy with another request")
      }
      request = Some(new InternetCard.Request(this, checkAddress(address), post))
      result(request.get)
    }
  }

  @Callback(direct = true, doc = """function():boolean -- Returns whether TCP connections can be made (config setting).""")
  def isTcpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(address:string[, port:number]):number -- Opens a new TCP connection. Returns the handle of the connection.""")
  def connect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val address = args.checkString(0)
    val port = if (args.count > 1) args.checkInteger(1) else -1
    if (!Settings.get.tcpEnabled) {
      return result(Unit, "tcp connections are unavailable")
    }
    if (connections.size >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }
    val url = checkUri(address)
    val socketAddress = new InetSocketAddress(url.getHost, if (url.getPort != -1) url.getPort else port)
    val channel = SocketChannel.open()
    channel.configureBlocking(false)
    channel.connect(socketAddress)
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(connections.contains).next()
    connections += handle -> channel
    result(handle)
  }

  @Callback(direct = true, doc = """function(handle:number) -- Closes an open socket stream.""")
  def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val handle = args.checkInteger(0)
    connections.remove(handle) match {
      case Some(socket) => socket.close()
      case _ => throw new IllegalArgumentException("bad connection descriptor")
    }
    null
  }

  @Callback(doc = """function(handle:number, data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.""")
  def write(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val handle = args.checkInteger(0)
    val value = args.checkByteArray(1)
    connections.get(handle) match {
      case Some(socket) =>
        if (socket.finishConnect()) result(socket.write(ByteBuffer.wrap(value)))
        else result(0)
      case _ => throw new IOException("bad connection descriptor")
    }
  }

  @Callback(doc = """function(handle:number, n:number):string -- Tries to read data from the socket stream. Returns the read byte array.""")
  def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val handle = args.checkInteger(0)
    val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.checkInteger(1)))
    connections.get(handle) match {
      case Some(socket) =>
        if (socket.finishConnect()) {
          val buffer = ByteBuffer.allocate(n)
          val read = socket.read(buffer)
          if (read == -1) null
          else result(buffer.array.view(0, read).toArray)
        }
        else result(Array.empty[Byte])
      case _ => throw new IOException("bad connection descriptor")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (owner.isEmpty && node.host.isInstanceOf[Context] && node.isNeighborOf(this.node)) {
      owner = Some(node.host.asInstanceOf[Context])
      romInternet.foreach(fs => node.connect(fs.node))
    }
  }

  override def onDisconnect(node: Node) = this.synchronized {
    super.onDisconnect(node)
    if (owner.isDefined && (node == this.node || node.host.isInstanceOf[Context] && (node.host.asInstanceOf[Context] == owner.get))) {
      owner = None
      for ((_, socket) <- connections) {
        socket.close()
      }
      connections.clear()
      request = None
      romInternet.foreach(_.node.remove())
    }
  }

  override def onMessage(message: Message) = this.synchronized {
    super.onMessage(message)
    message.data match {
      case Array() if (message.name == "computer.stopped" || message.name == "computer.started") && owner.isDefined && message.source.address == owner.get.node.address =>
        connections.values.foreach(_.close())
        connections.clear()
        this.synchronized {
          request = None
        }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romInternet.foreach(_.load(nbt.getCompoundTag("romInternet")))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romInternet.foreach(fs => nbt.setNewCompoundTag("romInternet", fs.save))
  }

  // ----------------------------------------------------------------------- //

  private def checkUri(address: String): URI = {
    try {
      val parsed = new URI(address)
      if (parsed.getHost != null && parsed.getPort != -1) {
        checkLists(Matcher.quoteReplacement(parsed.getHost))
        return parsed
      }
    }
    catch {
      case _: Throwable =>
    }

    val simple = new URI("oc://" + address)
    if (simple.getHost != null && simple.getPort != -1) {
      checkLists(Matcher.quoteReplacement(simple.getHost))
      return simple
    }

    throw new IllegalArgumentException("address could not be parsed")
  }

  private def checkAddress(address: String) = {
    val url = try new URL(address)
    catch {
      case e: Throwable => throw new FileNotFoundException("invalid address")
    }
    val protocol = url.getProtocol
    if (!protocol.matches("^https?$")) {
      throw new FileNotFoundException("unsupported protocol")
    }
    checkLists(Matcher.quoteReplacement(url.getHost))
    url
  }

  private def checkLists(host: String) {
    if (Settings.get.httpHostWhitelist.length > 0 && !Settings.get.httpHostWhitelist.exists(host.matches)) {
      throw new FileNotFoundException("domain is not whitelisted")
    }
    if (Settings.get.httpHostBlacklist.exists(host.matches)) {
      throw new FileNotFoundException("domain is blacklisted")
    }
  }
}

object InternetCard {
  private val threadPool = ThreadPoolFactory.create("HTTP", Settings.get.httpThreads)

  class Request(val owner: Option[InternetCard] = None) extends AbstractValue {
    def this(owner: InternetCard, url: URL, post: Option[String]) {
      this(Option(owner))
      this.url = url
      this.post = post
      scheduleRequest()
    }

    private var url: URL = null
    private var post: Option[String] = None
    private var data: Option[Array[Byte]] = None
    private var error: Option[String] = None

    @Callback
    def read(context: Context, args: Arguments): Array[AnyRef] = {
      if (data.isDefined) {
        val buffer = data.get
        if (buffer.length == 0) Array(Unit)
        else {
          val n = math.min(Settings.get.maxReadBuffer, if (args.count > 0) args.checkInteger(0) else Int.MaxValue)
          val count = math.min(n, buffer.length)
          val result = buffer.take(count)
          data = Some(buffer.drop(count))
          Array(result)
        }
      }
      else if (error.isDefined) Array(Unit, error.get)
      else Array("")
    }

    protected def scheduleRequest() {
      InternetCard.threadPool.submit(new Runnable {
        override def run() = try {
          val proxy = Option(MinecraftServer.getServer.getServerProxy).getOrElse(java.net.Proxy.NO_PROXY)
          url.openConnection(proxy) match {
            case http: HttpURLConnection => try {
              http.setDoInput(true)
              if (post.isDefined) {
                http.setRequestMethod("POST")
                http.setDoOutput(true)
                http.setReadTimeout(Settings.get.httpTimeout)

                val out = new BufferedWriter(new OutputStreamWriter(http.getOutputStream))
                out.write(post.get)
                out.close()
              }
              else {
                http.setRequestMethod("GET")
                http.setDoOutput(false)
              }

              val input = http.getInputStream
              val data = mutable.ArrayBuffer.empty[Byte]
              val buffer = Array.fill[Byte](Settings.get.maxNetworkPacketSize)(0)
              var count = 0
              do {
                count = input.read(buffer)
                if (count > 0) {
                  data ++= buffer.take(count)
                }
              } while (count != -1)
              input.close()
              this.synchronized {
                Request.this.data = Some(data.toArray)
              }
            }
            finally {
              http.disconnect()
            }
            case other => error = Some("connection failed")
          }
        }
        catch {
          case e: FileNotFoundException =>
            error = Some("not found: " + Option(e.getMessage).getOrElse(e.toString))
          case e: UnknownHostException =>
            error = Some("unknown host: " + Option(e.getMessage).getOrElse(e.toString))
          case _: SocketTimeoutException =>
            error = Some("timeout")
          case e: Throwable =>
            error = Some(Option(e.getMessage).getOrElse(e.toString))
        }
        finally {
          owner match {
            case Some(card) => card.synchronized {
              card.request = None
            }
            case _ =>
          }
        }
      })
    }

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey("url")) url = new URL(nbt.getString("url"))
      if (nbt.hasKey("post")) post = Option(nbt.getString("post"))
      if (nbt.hasKey("error")) error = Option(nbt.getString("error"))
      if (nbt.hasKey("data")) data = Option(nbt.getByteArray("data"))
      if (error.isEmpty && data.isEmpty) scheduleRequest()
    }

    override def save(nbt: NBTTagCompound) {
      if (url != null) nbt.setString("url", url.toString)
      if (post.isDefined) nbt.setString("post", post.get)
      if (error.isDefined) nbt.setString("error", error.get)
      if (data.isDefined) nbt.setByteArray("data", data.get)
    }
  }

}