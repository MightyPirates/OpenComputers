package li.cil.oc.server.component

import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util
import java.util.UUID
import java.util.concurrent._

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.server.MinecraftServer
import net.minecraftforge.fml.common.FMLCommonHandler

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class InternetCard extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  protected var owner: Option[Context] = None

  protected val connections = mutable.Set.empty[InternetCard.Closable]

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Communication,
    DeviceAttribute.Description -> "Internet modem",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "SuperLink X-D4NK"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():boolean -- Returns whether HTTP requests can be made (config setting).""")
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @Callback(doc = """function(url:string[, postData:string[, headers:table[, method:string]]]):userdata -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.""")
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
    val headers = if (args.isTable(2)) args.checkTable(2).collect {
      case (key: String, value: AnyRef) => (key, value.toString)
    }.toMap
    else Map.empty[String, String]
    if (!Settings.get.httpHeadersEnabled && headers.nonEmpty) {
      return result(Unit, "http request headers are unavailable")
    }
    val method = if (args.isString(3)) Option(args.checkString(3)) else None
    val request = new InternetCard.HTTPRequest(this, checkAddress(address), post, headers, method)
    connections += request
    result(request)
  }

  @Callback(direct = true, doc = """function():boolean -- Returns whether TCP connections can be made (config setting).""")
  def isTcpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.tcpEnabled)

  @Callback(doc = """function(address:string[, port:number]):userdata -- Opens a new TCP connection. Returns the handle of the connection.""")
  def connect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val address = args.checkString(0)
    val port = args.optInteger(1, -1)
    if (!Settings.get.tcpEnabled) {
      return result(Unit, "tcp connections are unavailable")
    }
    if (connections.size >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }
    val uri = checkUri(address, port)
    val socket = new InternetCard.TCPSocket(this, uri, port)
    connections += socket
    result(socket)
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
    }
  }

  override def onDisconnect(node: Node) = this.synchronized {
    super.onDisconnect(node)
    if (owner.isDefined && (node == this.node || node.host.isInstanceOf[Context] && (node.host.asInstanceOf[Context] == owner.get))) {
      owner = None
      this.synchronized {
        connections.foreach(_.close())
        connections.clear()
      }
    }
  }

  override def onMessage(message: Message) = this.synchronized {
    super.onMessage(message)
    message.data match {
      case Array() if (message.name == "computer.stopped" || message.name == "computer.started") && owner.isDefined && message.source.address == owner.get.node.address =>
        this.synchronized {
          connections.foreach(_.close())
          connections.clear()
        }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  private def checkUri(address: String, port: Int): URI = {
    try {
      val parsed = new URI(address)
      if (parsed.getHost != null && (parsed.getPort > 0 || port > 0)) {
        return parsed
      }
    }
    catch {
      case _: Throwable =>
    }

    val simple = new URI("oc://" + address)
    if (simple.getHost != null) {
      if (simple.getPort > 0)
        return simple
      else if (port > 0)
        return new URI(simple.toString + ":" + port)
    }

    throw new IllegalArgumentException("address could not be parsed or no valid port given")
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

  trait Closable {
    def close(): Unit
  }

  object TCPNotifier extends Thread {
    private var selector = Selector.open()
    private val toAccept = new ConcurrentLinkedQueue[(SocketChannel, () => Unit)]

    override def run(): Unit = {
      while (true) {
        try {
          Stream.continually(toAccept.poll).takeWhile(_ != null).foreach({
            case (channel: SocketChannel, action: (() => Unit)) =>
              channel.register(selector, SelectionKey.OP_READ, action)
          })

          selector.select()

          import scala.collection.JavaConversions._
          val selectedKeys = selector.selectedKeys
          val readableKeys = mutable.HashSet[SelectionKey]()
          selectedKeys.filter(_.isReadable).foreach(key => {
            key.attachment.asInstanceOf[() => Unit].apply()
            readableKeys += key
          })

          if(readableKeys.nonEmpty) {
            val newSelector = Selector.open()
            selectedKeys.filter(!readableKeys.contains(_)).foreach(key => {
              key.channel.register(newSelector, SelectionKey.OP_READ, key.attachment)
            })
            selector.close()
            selector = newSelector
          }
        } catch {
          case e: IOException =>
            OpenComputers.log.error("Error in TCP selector loop.", e)
        }
      }
    }

    def add(e: (SocketChannel, () => Unit)) {
      toAccept.offer(e)
      selector.wakeup()
    }
  }

  TCPNotifier.start()

  class TCPSocket extends AbstractValue with Closable {
    def this(owner: InternetCard, uri: URI, port: Int) {
      this()
      this.owner = Some(owner)
      channel = SocketChannel.open()
      channel.configureBlocking(false)
      address = threadPool.submit(new AddressResolver(uri, port))
    }

    private var owner: Option[InternetCard] = None
    private var address: Future[InetAddress] = null
    private var channel: SocketChannel = null
    private var isAddressResolved = false
    private val id = UUID.randomUUID()

    private def setupSelector() {
      if (channel == null) return
      TCPNotifier.add((channel, () => {
        owner match {
          case Some(internetCard) =>
            internetCard.node.sendToVisible("computer.signal", "internet_ready", id.toString)
          case _ =>
            channel.close()
        }
      }))
    }

    @Callback(doc = """function():boolean -- Ensures a socket is connected. Errors if the connection failed.""")
    def finishConnect(context: Context, args: Arguments): Array[AnyRef] = {
      val r = this.synchronized(result(checkConnected()))
      setupSelector()
      r
    }

    @Callback(doc = """function([n:number]):string -- Tries to read data from the socket stream. Returns the read byte array.""")
    def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.optInteger(0, Int.MaxValue)))
      if (checkConnected()) {
        val buffer = ByteBuffer.allocate(n)
        val read = channel.read(buffer)
        if (read == -1) result(Unit)
        else {
          setupSelector()
          result(buffer.array.view(0, read).toArray)
        }
      }
      else result(Array.empty[Byte])
    }

    @Callback(doc = """function(data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.""")
    def write(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (checkConnected()) {
        val value = args.checkByteArray(0)
        result(channel.write(ByteBuffer.wrap(value)))
      }
      else result(0)
    }

    @Callback(direct = true, doc = """function() -- Closes an open socket stream.""")
    def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      close()
      null
    }

    @Callback(direct = true, doc = """function():string -- Returns connection ID.""")
    def id(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(id.toString)
    }

    override def dispose(context: Context): Unit = {
      super.dispose(context)
      close()
    }

    override def close(): Unit = {
      owner.foreach(card => {
        card.connections.remove(this)
        address.cancel(true)
        channel.close()
        owner = None
        address = null
        channel = null
      })
    }

    private def checkConnected() = {
      if (owner.isEmpty) throw new IOException("connection lost")
      try {
        if (isAddressResolved) channel.finishConnect()
        else if (address.isCancelled) {
          // I don't think this can ever happen, Justin Case.
          channel.close()
          throw new IOException("bad connection descriptor")
        }
        else if (address.isDone) {
          // Check for errors.
          try address.get catch {
            case e: ExecutionException => throw e.getCause
          }
          isAddressResolved = true
          false
        }
        else false
      }
      catch {
        case t: Throwable =>
          close()
          false
      }
    }

    // This has to be an explicit internal class instead of an anonymous one
    // because the scala compiler breaks otherwise. Yay for compiler bugs.
    private class AddressResolver(val uri: URI, val port: Int) extends Callable[InetAddress] {
      override def call(): InetAddress = {
        val resolved = InetAddress.getByName(uri.getHost)
        checkLists(resolved, uri.getHost)
        val address = new InetSocketAddress(resolved, if (uri.getPort != -1) uri.getPort else port)
        channel.connect(address)
        resolved
      }
    }

  }

  def checkLists(inetAddress: InetAddress, host: String) {
    if (Settings.get.httpHostWhitelist.length > 0 && !Settings.get.httpHostWhitelist.exists(i => i.apply(inetAddress, host).getOrElse(false))) {
      throw new FileNotFoundException("address is not whitelisted")
    }
    if (Settings.get.httpHostBlacklist.length > 0 && Settings.get.httpHostBlacklist.exists(i => i.apply(inetAddress, host).getOrElse(true))) {
      throw new FileNotFoundException("address is blacklisted")
    }
  }

  class HTTPRequest extends AbstractValue with Closable {
    def this(owner: InternetCard, url: URL, post: Option[String], headers: Map[String, String], method: Option[String]) {
      this()
      this.owner = Some(owner)
      this.stream = threadPool.submit(new RequestSender(url, post, headers, method))
    }

    private var owner: Option[InternetCard] = None
    private var response: Option[(Int, String, AnyRef)] = None
    private var stream: Future[InputStream] = null
    private val queue = new ConcurrentLinkedQueue[Byte]()
    private var reader: Future[_] = null
    private var eof = false

    @Callback(doc = """function():boolean -- Ensures a response is available. Errors if the connection failed.""")
    def finishConnect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized(result(checkResponse()))

    @Callback(direct = true, doc = """function():number, string, table -- Get response code, message and headers.""")
    def response(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      response match {
        case Some((code, message, headers)) => result(code, message, headers)
        case _ => result(Unit)
      }
    }

    @Callback(doc = """function([n:number]):string -- Tries to read data from the response. Returns the read byte array.""")
    def read(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.optInteger(0, Int.MaxValue)))
      if (checkResponse()) {
        if (eof && queue.isEmpty) result(Unit)
        else {
          val buffer = ByteBuffer.allocate(n)
          var read = 0
          while (!queue.isEmpty && read < n) {
            buffer.put(queue.poll())
            read += 1
          }
          if (read == 0) {
            readMore()
          }
          result(buffer.array.view(0, read).toArray)
        }
      }
      else result(Array.empty[Byte])
    }

    @Callback(direct = true, doc = """function() -- Closes an open socket stream.""")
    def close(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      close()
      null
    }

    override def dispose(context: Context): Unit = {
      super.dispose(context)
      close()
    }

    override def close(): Unit = {
      owner.foreach(card => {
        card.connections.remove(this)
        stream.cancel(true)
        if (reader != null) {
          reader.cancel(true)
        }
        owner = None
        stream = null
        reader = null
      })
    }

    private def checkResponse() = this.synchronized {
      if (owner.isEmpty) throw new IOException("connection lost")
      if (stream.isDone) {
        if (reader == null) {
          // Check for errors.
          try stream.get catch {
            case e: ExecutionException => throw e.getCause
          }
          readMore()
        }
        true
      }
      else false
    }

    private def readMore(): Unit = {
      if (reader == null || reader.isCancelled || reader.isDone) {
        if (!eof) reader = threadPool.submit(new Runnable {
          override def run(): Unit = {
            val buffer = new Array[Byte](Settings.get.maxReadBuffer)
            val count = stream.get.read(buffer)
            if (count < 0) {
              eof = true
            }
            for (i <- 0 until count) {
              queue.add(buffer(i))
            }
          }
        })
      }
    }

    // This one doesn't (see comment in TCP socket), but I like to keep it consistent.
    private class RequestSender(val url: URL, val post: Option[String], val headers: Map[String, String], val method: Option[String]) extends Callable[InputStream] {
      override def call() = try {
        checkLists(InetAddress.getByName(url.getHost), url.getHost)
        val proxy = Option(FMLCommonHandler.instance.getMinecraftServerInstance.getServerProxy).getOrElse(java.net.Proxy.NO_PROXY)
        url.openConnection(proxy) match {
          case http: HttpURLConnection => try {
            http.setDoInput(true)
            http.setDoOutput(post.isDefined)
            http.setRequestMethod(if (method.isDefined) method.get else if (post.isDefined) "POST" else "GET")
            headers.foreach(Function.tupled(http.setRequestProperty))
            if (post.isDefined) {
              http.setReadTimeout(Settings.get.httpTimeout)

              val out = new BufferedWriter(new OutputStreamWriter(http.getOutputStream))
              out.write(post.get)
              out.close()
            }

            val input = http.getInputStream
            HTTPRequest.this.synchronized {
              response = Some((http.getResponseCode, http.getResponseMessage, http.getHeaderFields))
            }
            input
          }
          catch {
            case t: Throwable =>
              http.disconnect()
              throw t
          }
          case other => throw new IOException("unexpected connection type")
        }
      }
      catch {
        case e: UnknownHostException =>
          throw new IOException("unknown host: " + Option(e.getMessage).getOrElse(e.toString))
        case e: Throwable =>
          throw new IOException(Option(e.getMessage).getOrElse(e.toString))
      }
    }

  }

}
