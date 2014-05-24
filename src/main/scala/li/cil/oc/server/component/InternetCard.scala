package li.cil.oc.server.component

import java.io.{IOException, OutputStreamWriter, BufferedWriter, FileNotFoundException}
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.{ExecutionException, Callable}
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.component.ManagedComponent
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import scala.collection.mutable

class InternetCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  val romInternet = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/internet"), "internet"))

  protected var owner: Option[Context] = None

  protected val connections = mutable.Map.empty[Int, InternetCard.Socket]

  protected var request: Option[InternetCard.Request] = None

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether HTTP requests can be made (config setting).""")
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(url:string[, postData:string]):boolean -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.""")
  def request(context: Context, args: Arguments): Array[AnyRef] = {
    checkOwner(context)
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
    checkOwner(context)
    val address = args.checkString(0)
    val port = if (args.count > 1) args.checkInteger(1) else -1
    if (!Settings.get.tcpEnabled) {
      return result(Unit, "tcp connections are unavailable")
    }
    if (connections.size >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }
    val uri = checkUri(address)
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).
      filterNot(connections.contains).
      next()
    connections += handle -> new InternetCard.Socket(uri, port)
    result(handle)
  }

  @Callback(direct = true, doc = """function(handle:number) -- Closes an open socket stream.""")
  def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val handle = args.checkInteger(0)
    connections.remove(handle) match {
      case Some(socket) => socket.close()
      case _ => throw new IllegalArgumentException("bad connection descriptor")
    }
    null
  }

  @Callback(doc = """function(handle:number, data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.""")
  def write(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val handle = args.checkInteger(0)
    val value = args.checkByteArray(1)
    connections.get(handle) match {
      case Some(socket) => result(socket.write(value))
      case _ => throw new IOException("bad connection descriptor")
    }
  }

  @Callback(doc = """function(handle:number, n:number):string -- Tries to read data from the socket stream. Returns the read byte array.""")
  def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val handle = args.checkInteger(0)
    val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.checkInteger(1)))
    connections.get(handle) match {
      case Some(socket) => result(socket.read(n))
      case _ => throw new IOException("bad connection descriptor")
    }
  }

  private def checkOwner(context: Context) {
    if (owner.isEmpty || context.node != owner.get.node) {
      throw new IllegalArgumentException("can only be used by the owning computer")
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
        return parsed
      }
    }
    catch {
      case _: Throwable =>
    }

    val simple = new URI("oc://" + address)
    if (simple.getHost != null && simple.getPort != -1) {
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
    url
  }
}

object InternetCard {
  private val threadPool = ThreadPoolFactory.create("Internet", Settings.get.internetThreads)

  def checkLists(inetAddress: InetAddress, host: String) {
    if (Settings.get.httpHostWhitelist.length > 0 && !Settings.get.httpHostWhitelist.exists(_(inetAddress, host))) {
      throw new FileNotFoundException("address is not whitelisted")
    }
    if (Settings.get.httpHostBlacklist.exists(_(inetAddress, host))) {
      throw new FileNotFoundException("address is blacklisted")
    }
  }

  class Socket(val uri: URI, val port: Int) {
    val address = threadPool.submit(new Callable[InetAddress] {
      override def call() = {
        val resolved = InetAddress.getByName(uri.getHost)
        checkLists(resolved, uri.getHost)
        resolved
      }
    })
    val channel = SocketChannel.open()
    channel.configureBlocking(false)
    var isConnecting = false

    def close() {
      channel.close()
    }

    def read(n: Int) = {
      if (checkConnected()) {
        val buffer = ByteBuffer.allocate(n)
        val read = channel.read(buffer)
        if (read == -1) null
        else buffer.array.view(0, read).toArray
      }
      else Array.empty[Byte]
    }

    def write(value: Array[Byte]) = {
      if (checkConnected()) channel.write(ByteBuffer.wrap(value))
      else 0
    }

    def checkConnected() = {
      if (isConnecting) channel.finishConnect()
      else if (address.isCancelled) {
        // I don't think this can ever happen, Justin Case.
        close()
        throw new IOException("bad connection descriptor")
      }
      else if (address.isDone) try {
        val socketAddress = new InetSocketAddress(address.get, if (uri.getPort != -1) uri.getPort else port)
        channel.connect(socketAddress)
        isConnecting = true
        false
      }
      catch {
        case e: ExecutionException => throw e.getCause
      }
      else false
    }
  }

  class Request(val owner: Option[InternetCard] = None) extends AbstractValue {
    private var url: URL = null
    private var post: Option[String] = None
    private var data: Option[Array[Byte]] = None
    private var error: Option[String] = None

    def this(owner: InternetCard, url: URL, post: Option[String]) {
      this(Option(owner))
      this.url = url
      this.post = post
      scheduleRequest()
    }

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

    protected def scheduleRequest() {
      InternetCard.threadPool.submit(new Runnable {
        override def run() = try {
          checkLists(InetAddress.getByName(url.getHost), url.getHost)
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
              } while (count != -1 && data.length < Settings.get.httpMaxDownloadSize)
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
  }

}