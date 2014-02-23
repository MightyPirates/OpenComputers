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

class InternetCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  val romInternet = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/internet"), "internet"))

  protected var owner: Option[Context] = None

  protected val connections = mutable.Map.empty[Int, SocketChannel]

  // For HTTP requests the state switches like so:
  // Pre: request == None && queue == None
  // request = value
  // thread {
  //   queue = value
  //   request = None
  // }
  // while (request == None && queue contains elements) { signal(queue.pop()) }
  // queue = None
  // Post: request == None && queue == None

  protected var request: Option[(String, Option[String])] = None

  protected var queue: Option[(String, mutable.Queue[Array[Byte]])] = None

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether HTTP requests can be made (config setting).""")
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function():boolean -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.""")
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
      if (request.isDefined || queue.isDefined) {
        return result(Unit, "already busy with another request")
      }
      scheduleRequest(address, post)
    }
    result(true)
  }

  protected def scheduleRequest(address: String, post: Option[String]) {
    val url = checkAddress(address)
    request = Some((address, post))
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
            queue = Some(address -> (mutable.Queue(data.toArray.grouped(Settings.get.maxNetworkPacketSize).toSeq: _*) ++ Iterable(null)))
          }
          finally {
            http.disconnect()
          }
          case other => owner.foreach(_.signal("http_response", address, Unit, "connection failed"))
        }
      }
      catch {
        case e: FileNotFoundException =>
          owner.foreach(_.signal("http_response", address, Unit, "not found: " + Option(e.getMessage).getOrElse(e.toString)))
        case _: SocketTimeoutException =>
          owner.foreach(_.signal("http_response", address, Unit, "timeout"))
        case e: Throwable =>
          owner.foreach(_.signal("http_response", address, Unit, Option(e.getMessage).getOrElse(e.toString)))
      }
      finally {
        InternetCard.this.synchronized {
          if (request.isDefined) {
            request = None
          }
          else {
            // Got disconnected in the meantime.
            queue = None
          }
        }
      }
    })
  }

  @Callback(direct = true, doc = """function():boolean -- Returns whether TCP connections can be made (config setting).""")
  def isTcpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(address:string[, port:number]):number -- Opens a new TCP connection. Returns the handle of the connection.""")
  def connect(context: Context, args: Arguments): Array[AnyRef] = {
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

  @Callback(doc = """function(handle:number) -- Closes an open socket stream.""")
  def close(context: Context, args: Arguments): Array[AnyRef] = {
    val handle = args.checkInteger(0)
    connections.remove(handle) match {
      case Some(socket) => socket.close()
      case _ => throw new IllegalArgumentException("bad connection descriptor")
    }
    null
  }

  @Callback(doc = """function(handle:number, data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.""")
  def write(context: Context, args: Arguments): Array[AnyRef] = {
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
  def read(context: Context, args: Arguments): Array[AnyRef] = {
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

  override val canUpdate = true

  override def update() {
    super.update()

    this.synchronized {
      if (request.isEmpty && queue.isDefined) {
        val (address, packets) = queue.get
        if (owner.fold(true)(_.signal("http_response", address, packets.front))) {
          packets.dequeue()
        }
        if (packets.isEmpty) {
          queue = None
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (owner.isEmpty && node.host.isInstanceOf[Context] && node.isNeighborOf(this.node)) {
      owner = Some(node.host.asInstanceOf[Context])
      romInternet.foreach(rom => node.connect(rom.node))
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (owner.isDefined && (node == this.node || node.host.isInstanceOf[Context] && (node.host.asInstanceOf[Context] == owner.get))) {
      owner = None
      for ((_, socket) <- connections) {
        socket.close()
      }
      connections.clear()
      request = None
      queue = None
      romInternet.foreach(_.node.remove())
    }
  }

  override def onMessage(message: Message) {
    super.onMessage(message)
    message.data match {
      case Array() if (message.name == "computer.stopped" || message.name == "computer.started") && owner.isDefined && message.source.address == owner.get.node.address =>
        connections.values.foreach(_.close())
        connections.clear()
        InternetCard.this.synchronized {
          request = None
          queue = None
        }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romInternet.foreach(_.load(nbt.getCompoundTag("romInternet")))
    if (nbt.hasKey("url")) {
      val address = nbt.getString("url")
      val data = nbt.getByteArray("data")
      queue = Some(address -> (mutable.Queue(data.grouped(Settings.get.maxNetworkPacketSize).toSeq: _*) ++ Iterable(null)))
    }
    if (nbt.hasKey("request")) {
      val address = nbt.getString("request")
      val post =
        if (nbt.hasKey("postData")) Option(nbt.getString("postData"))
        else None
      // Restart request?
      if (!queue.isDefined) {
        scheduleRequest(address, post)
      }
      // Otherwise this should have been None anyway...
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romInternet.foreach(rom => nbt.setNewCompoundTag("romInternet", rom.save))
    this.synchronized {
      request match {
        case Some((address, data)) =>
          nbt.setString("request", address)
          data match {
            case Some(value) => nbt.setString("postData", value)
            case _ =>
          }
        case _ =>
      }
      queue match {
        case Some((address, packets)) =>
          nbt.setString("url", address)
          val data = mutable.ArrayBuffer.empty[Byte]
          packets.toIterable.dropRight(1).foreach(data.appendAll(_))
          nbt.setByteArray("data", data.toArray)
        case _ =>
      }
    }
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
}