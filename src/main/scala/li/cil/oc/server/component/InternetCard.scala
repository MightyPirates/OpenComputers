package li.cil.oc.server.component

import java.io.{BufferedWriter, FileNotFoundException, IOException, OutputStreamWriter}
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.{Callable, ExecutionException}

import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ThreadPoolFactory
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer

import scala.collection.mutable

class InternetCard extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  val romInternet = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/internet"), "internet"))

  protected var owner: Option[Context] = None

  protected val connections = mutable.Map.empty[Int, InternetCard.Connection]

  private def newHandle() = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).
    filterNot(connections.contains).
    next()

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether HTTP requests can be made (config setting).""")
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(url:string[, postData:string]):boolean -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.""")
  def request(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val address = args.checkString(0)
    if (!Settings.get.httpEnabled) {
      return result(Unit, "http requests are unavailable")
    }
    if (connections.size >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }
    val post = if (args.isString(1)) Option(args.checkString(1)) else None
    val handle = newHandle()
    connections += handle -> new InternetCard.Request(checkAddress(address), post)
    result(handle)
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
    val handle = newHandle()
    connections += handle -> new InternetCard.Socket(uri, port)
    result(handle)
  }

  @Callback(doc = """function(handle:number):boolean -- Ensures a socket is connected. Errors if the connection failed.""")
  def finishConnect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val handle = args.checkInteger(0)
    connections.get(handle) match {
      case Some(connection) => result(connection.checkConnected())
      case _ => throw new IOException("bad connection descriptor")
    }
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
      case Some(connection) => result(connection.write(value))
      case _ => throw new IOException("bad connection descriptor")
    }
  }

  @Callback(doc = """function(handle:number[, n:number]):string -- Tries to read data from the socket stream. Returns the read byte array.""")
  def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val handle = args.checkInteger(0)
    val n = math.min(Settings.get.maxReadBuffer, math.max(0, if (args.count > 1) args.checkInteger(1) else Int.MaxValue))
    connections.get(handle) match {
      case Some(connection) => result(connection.read(n))
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
      this.synchronized {
        for ((_, socket) <- connections) {
          socket.close()
        }
        connections.clear()
      }
      romInternet.foreach(_.node.remove())
    }
  }

  override def onMessage(message: Message) = this.synchronized {
    super.onMessage(message)
    message.data match {
      case Array() if (message.name == "computer.stopped" || message.name == "computer.started") && owner.isDefined && message.source.address == owner.get.node.address =>
        this.synchronized {
          connections.values.foreach(_.close())
          connections.clear()
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
    if (Settings.get.httpHostBlacklist.length > 0 && Settings.get.httpHostBlacklist.exists(_(inetAddress, host))) {
      throw new FileNotFoundException("address is blacklisted")
    }
  }

  trait Connection {
    def close()

    def read(n: Int): Array[Byte]

    def write(value: Array[Byte]): Int

    def checkConnected(): Boolean
  }

  class Socket(val uri: URI, val port: Int) extends Connection {
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

  class Request(val url: URL, val post: Option[String]) extends Connection with Runnable {
    private var data: Option[Array[Byte]] = None
    private var error: Option[String] = None

    // Perform actual request in a separate thread.
    InternetCard.threadPool.submit(this)

    override def close() {}

    override def read(n: Int) = this.synchronized {
      if (checkConnected()) {
        val buffer = data.get
        if (buffer.length == 0) null
        else {
          val count = math.min(n, buffer.length)
          val result = buffer.take(count)
          data = Option(buffer.drop(count))
          result
        }
      }
      else Array()
    }

    override def write(value: Array[Byte]) = throw new IOException("unsupported operation")

    override def checkConnected() = this.synchronized {
      if (data.isDefined) true
      else error match {
        case Some(reason) => throw new IOException(reason)
        case _ => false
      }
    }

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
        case other => this.synchronized(error = Some("connection failed"))
      }
    }
    catch {
      case e: FileNotFoundException =>
        this.synchronized(error = Some("not found: " + Option(e.getMessage).getOrElse(e.toString)))
      case e: UnknownHostException =>
        this.synchronized(error = Some("unknown host: " + Option(e.getMessage).getOrElse(e.toString)))
      case _: SocketTimeoutException =>
        this.synchronized(error = Some("timeout"))
      case e: Throwable =>
        this.synchronized(error = Some(Option(e.getMessage).getOrElse(e.toString)))
    }
  }

}