package li.cil.oc.server.component

import com.google.common.net.InetAddresses

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
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.SSLException
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.server.MinecraftServer

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class InternetCard extends prefab.ManagedEnvironment with DeviceInfo {
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
    if (!Settings.get.internetAccessAllowed()) {
      return result(Unit, "internet access is unavailable")
    }
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

  @Callback(direct = true, doc = """function():boolean -- Returns whether WebSocket connections can be made (config setting).""")
  def isWebSocketEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.webSocketEnabled)

  @Callback(doc = """function(address:string[, port:number]):userdata -- Opens a new TCP connection. Returns the handle of the connection.""")
  def connect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val address = args.checkString(0)
    val port = args.optInteger(1, -1)
    if (!Settings.get.internetAccessAllowed()) {
      return result(Unit, "internet access is unavailable")
    }
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

  @Callback(doc = """function(url:string[, headers:table]):userdata -- Opens a new WebSocket connection. Returns the handle of the connection.""")
  def websocket(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    checkOwner(context)
    val url = args.checkString(0)
    if (!Settings.get.internetAccessAllowed()) {
      return result(Unit, "internet access is unavailable")
    }
    if (!Settings.get.webSocketEnabled) {
      return result(Unit, "websocket connections are unavailable")
    }
    if (connections.size >= Settings.get.maxConnections) {
      throw new IOException("too many open connections")
    }
    val headers = if (args.isTable(1)) args.checkTable(1).collect {
      case (key: String, value: AnyRef) => (key, value.toString)
    }.toMap
    else Map.empty[String, String]
    if (!Settings.get.httpHeadersEnabled && headers.nonEmpty) {
      return result(Unit, "websocket request headers are unavailable")
    }
    val wsUrl = checkWebSocketAddress(url)
    val websocket = new InternetCard.WebSocketConnection(this, wsUrl, headers)
    connections += websocket
    result(websocket)
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

  private def checkWebSocketAddress(address: String) = {
    val url = try new URL(address.replaceFirst("^ws", "http"))
    catch {
      case e: Throwable => throw new FileNotFoundException("invalid websocket address")
    }
    val originalProtocol = address.split("://", 2)(0).toLowerCase
    if (!originalProtocol.matches("^wss?$")) {
      throw new FileNotFoundException("unsupported websocket protocol")
    }
    (url, originalProtocol == "wss")
  }
}

object InternetCard {
  // For InternetFilteringRuleTest, where Settings.get is not provided.
  private val threadPool = ThreadPoolFactory.create("Internet", Option(Settings.get) match {
    case None => 1
    case Some(settings) => settings.internetThreads
  })

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
            selector.keys.filter(!readableKeys.contains(_)).foreach(key => {
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
          throw t  // Пробрасываем исключение вместо возврата false
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

  def isRequestAllowed(settings: Settings, inetAddress: InetAddress, host: String): Boolean = {
    if (!settings.internetAccessAllowed()) {
      false
    } else {
      val rules = settings.internetFilteringRules
      inetAddress match {
        // IPv6 handling
        case inet6Address: Inet6Address =>
          // If the IP address is an IPv6 address with an embedded IPv4 address, and the IPv4 address is blocked,
          // block this request.
          if (InetAddresses.hasEmbeddedIPv4ClientAddress(inet6Address)) {
            val inet4in6Address = InetAddresses.getEmbeddedIPv4ClientAddress(inet6Address)
            if (!rules.map(r => r.apply(inet4in6Address, host)).collectFirst({ case Some(r) => r }).getOrElse(true)) {
              return false
            }
          }

          // Process address as an IPv6 address.
          rules.map(r => r.apply(inet6Address, host)).collectFirst({ case Some(r) => r }).getOrElse(false)
        // IPv4 handling
        case inet4Address: Inet4Address =>
          // Process address as an IPv4 address.
          rules.map(r => r.apply(inet4Address, host)).collectFirst({ case Some(r) => r }).getOrElse(false)
        case _ =>
          // Unrecognized address type - block.
          OpenComputers.log.warn("Internet Card blocked unrecognized address type: " + inetAddress.toString)
          false
      }
    }
  }

  def checkLists(inetAddress: InetAddress, host: String): Unit = {
    if (!isRequestAllowed(Settings.get, inetAddress, host)) {
      throw new FileNotFoundException("address is not allowed")
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
        val proxy = Option(MinecraftServer.getServer.getServerProxy).getOrElse(java.net.Proxy.NO_PROXY)
        url.openConnection(proxy) match {
          case http: HttpURLConnection => try {
            http.setDoInput(true)
            http.setDoOutput(post.isDefined)
            http.setRequestMethod(if (method.isDefined) method.get else if (post.isDefined) "POST" else "GET")
            http.setRequestProperty("User-Agent", Settings.get.httpUserAgent.replace("$version", OpenComputers.Version))
            headers.foreach(Function.tupled(http.setRequestProperty))
            if (post.isDefined) {
              http.setReadTimeout(Settings.get.httpTimeout)

              val out = new BufferedWriter(new OutputStreamWriter(http.getOutputStream))
              out.write(post.get)
              out.close()
            }

            HTTPRequest.this.synchronized {
              response = Some((http.getResponseCode, http.getResponseMessage, http.getHeaderFields))
            }

            // For successful responses (2xx), use getInputStream()
            // For error responses (4xx, 5xx), use getErrorStream() if available, otherwise return empty stream
            val responseCode = http.getResponseCode
            if (responseCode >= 200 && responseCode < 300) {
              http.getInputStream
            } else {
              // For HTTP error responses, try to get the error stream
              val errorStream = http.getErrorStream
              if (errorStream != null) {
                errorStream
              } else {
                // If no error stream is available, return an empty stream
                new java.io.ByteArrayInputStream(Array.empty[Byte])
              }
            }
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

  class WebSocketConnection extends AbstractValue with Closable {
    def this(owner: InternetCard, urlAndSecure: (URL, Boolean), headers: Map[String, String]) {
      this()
      this.owner = Some(owner)
      this.url = urlAndSecure._1
      this.isSecure = urlAndSecure._2
      this.headers = headers
      this.connector = threadPool.submit(new WebSocketConnector())
    }

    private var owner: Option[InternetCard] = None
    private var url: URL = null
    private var isSecure: Boolean = false
    private var headers: Map[String, String] = Map.empty
    private val id = UUID.randomUUID()
    private var connector: Future[SocketChannel] = null
    private var channel: SocketChannel = null
    private var sslEngine: SSLEngine = null
    private var sslInbound: ByteBuffer = null
    private var sslOutbound: ByteBuffer = null
    private var connected = false
    private val messageQueue = new ConcurrentLinkedQueue[String]()
    private val binaryQueue = new ConcurrentLinkedQueue[Array[Byte]]()
    private var reader: Future[_] = null
    private var handshakeComplete = false

    @Callback(doc = """function():boolean -- Ensures WebSocket connection is established. Errors if the connection failed.""")
    def finishConnect(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(checkConnected())
    }

    @Callback(doc = """function():string -- Returns connection ID.""")
    def id(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(id.toString)
    }

    @Callback(doc = """function():boolean -- Returns whether the WebSocket is connected.""")
    def isConnected(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      result(connected && handshakeComplete)
    }

    @Callback(doc = """function(message:string) -- Sends a text message over the WebSocket.""")
    def send(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (!connected || !handshakeComplete) {
        return result(false, "websocket not connected")
      }
      val message = args.checkString(0)
      try {
        sendWebSocketFrame(message, isText = true)
        result(true)
      } catch {
        case e: Exception =>
          result(false, e.getMessage)
      }
    }

    @Callback(doc = """function(data:string) -- Sends binary data over the WebSocket.""")
    def sendBinary(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      if (!connected || !handshakeComplete) {
        return result(false, "websocket not connected")
      }
      val data = args.checkByteArray(0)
      try {
        sendWebSocketFrame(data, isText = false)
        result(true)
      } catch {
        case e: Exception =>
          result(false, e.getMessage)
      }
    }

    @Callback(doc = """function():string -- Receives a text message from the WebSocket.""")
    def receive(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val message = messageQueue.poll()
      if (message != null) {
        result(message)
      } else {
        result(Unit)
      }
    }

    @Callback(doc = """function():string -- Receives binary data from the WebSocket.""")
    def receiveBinary(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
      val data = binaryQueue.poll()
      if (data != null) {
        result(data)
      } else {
        result(Unit)
      }
    }

    @Callback(direct = true, doc = """function() -- Closes the WebSocket connection.""")
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
        if (connector != null) connector.cancel(true)
        if (reader != null) reader.cancel(true)

        // Clean up SSL resources safely
        try {
          if (sslEngine != null) {
            sslEngine.closeOutbound()
          }
        } catch {
          case _: Throwable => // Ignore all cleanup errors
        }

        if (channel != null) channel.close()

        // Reset state variables
        connected = false
        handshakeComplete = false
        owner = None
        connector = null
        channel = null
        reader = null
        // Don't reset SSL variables to avoid setter issues
      })
    }



    private def checkConnected(): Boolean = {
      if (owner.isEmpty) throw new IOException("connection lost")
      if (connector != null && connector.isDone && !connected) {
        try {
          channel = connector.get()
          if (isSecure) {
            initializeSSL()
            performSSLHandshake()
          }
          connected = true
          performHandshake()
          startReading()
        } catch {
          case e: ExecutionException =>
            close()
            throw e.getCause
          case e: Exception =>
            close()
            throw e
        }
      }
      connected && handshakeComplete
    }

    private def initializeSSL(): Unit = {
      val sslContext = SSLContext.getDefault
      sslEngine = sslContext.createSSLEngine(url.getHost, if (url.getPort != -1) url.getPort else 443)
      sslEngine.setUseClientMode(true)

      val session = sslEngine.getSession
      val bufferSize = session.getPacketBufferSize
      sslInbound = ByteBuffer.allocate(bufferSize)
      sslOutbound = ByteBuffer.allocate(bufferSize)
    }

    private def performSSLHandshake(): Unit = {
      if (sslEngine == null || sslInbound == null || sslOutbound == null) {
        throw new SSLException("SSL not properly initialized")
      }

      sslEngine.beginHandshake()

      var handshakeStatus = sslEngine.getHandshakeStatus
      val appBuffer = ByteBuffer.allocate(sslEngine.getSession.getApplicationBufferSize)

      while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
             handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

        handshakeStatus match {
          case SSLEngineResult.HandshakeStatus.NEED_WRAP =>
            sslOutbound.clear()
            val result = sslEngine.wrap(ByteBuffer.allocate(0), sslOutbound)
            handshakeStatus = result.getHandshakeStatus
            sslOutbound.flip()
            if (sslOutbound.hasRemaining) {
              channel.write(sslOutbound)
            }

          case SSLEngineResult.HandshakeStatus.NEED_UNWRAP =>
            if (channel.read(sslInbound) < 0) {
              throw new SSLException("SSL handshake failed: connection closed")
            }
            sslInbound.flip()
            val result = sslEngine.unwrap(sslInbound, appBuffer)
            handshakeStatus = result.getHandshakeStatus
            sslInbound.compact()

          case SSLEngineResult.HandshakeStatus.NEED_TASK =>
            var task = sslEngine.getDelegatedTask
            while (task != null) {
              task.run()
              task = sslEngine.getDelegatedTask
            }
            handshakeStatus = sslEngine.getHandshakeStatus

          case _ =>
            throw new SSLException("Unknown handshake status: " + handshakeStatus)
        }
      }
    }

    private def sslWrite(data: ByteBuffer): Unit = {
      if (isSecure && sslEngine != null && sslOutbound != null) {
        sslOutbound.clear()
        val result = sslEngine.wrap(data, sslOutbound)
        if (result.getStatus != SSLEngineResult.Status.OK) {
          throw new SSLException("SSL wrap failed: " + result.getStatus)
        }
        sslOutbound.flip()
        while (sslOutbound.hasRemaining) {
          channel.write(sslOutbound)
        }
      } else {
        channel.write(data)
      }
    }

    private def sslRead(buffer: ByteBuffer): Int = {
      if (isSecure && sslEngine != null) {
        val netData = ByteBuffer.allocate(sslEngine.getSession.getPacketBufferSize)
        val bytesRead = channel.read(netData)
        if (bytesRead > 0) {
          netData.flip()
          val result = sslEngine.unwrap(netData, buffer)
          if (result.getStatus != SSLEngineResult.Status.OK) {
            throw new SSLException("SSL unwrap failed: " + result.getStatus)
          }
          result.bytesProduced()
        } else {
          bytesRead
        }
      } else {
        channel.read(buffer)
      }
    }

    private def performHandshake(): Unit = {
      // Simplified WebSocket handshake implementation
      val key = java.util.Base64.getEncoder.encodeToString(java.security.SecureRandom.getInstanceStrong.generateSeed(16))
      val request = new StringBuilder()
      request.append(s"GET ${url.getPath}${if (url.getQuery != null) "?" + url.getQuery else ""} HTTP/1.1\r\n")
      request.append(s"Host: ${url.getHost}${if (url.getPort != -1) ":" + url.getPort else ""}\r\n")
      request.append("Upgrade: websocket\r\n")
      request.append("Connection: Upgrade\r\n")
      request.append(s"Sec-WebSocket-Key: $key\r\n")
      request.append("Sec-WebSocket-Version: 13\r\n")
      headers.foreach { case (k, v) => request.append(s"$k: $v\r\n") }
      request.append("\r\n")

      sslWrite(ByteBuffer.wrap(request.toString.getBytes("UTF-8")))

      // Read response (simplified)
      val buffer = ByteBuffer.allocate(4096)
      sslRead(buffer)
      val response = new String(buffer.array(), 0, buffer.position(), "UTF-8")

      if (response.contains("HTTP/1.1 101") && response.contains("Upgrade: websocket")) {
        handshakeComplete = true
        owner.foreach(_.node.sendToVisible("computer.signal", "websocket_success", id.toString))
      } else {
        throw new IOException("WebSocket handshake failed")
      }
    }

    private def startReading(): Unit = {
      reader = threadPool.submit(new Runnable {
        override def run(): Unit = {
          try {
            while (connected && !Thread.currentThread().isInterrupted) {
              readWebSocketFrame()
            }
          } catch {
            case _: InterruptedException => // Expected when closing
            case e: Exception =>
              owner.foreach(_.node.sendToVisible("computer.signal", "websocket_error", id.toString, e.getMessage))
              close()
          }
        }
      })
    }

    private def readWebSocketFrame(): Unit = {
      // Simplified WebSocket frame reading
      val headerBuffer = ByteBuffer.allocate(2)
      if (sslRead(headerBuffer) < 2) return

      headerBuffer.flip()
      val firstByte = headerBuffer.get() & 0xFF
      val secondByte = headerBuffer.get() & 0xFF

      val fin = (firstByte & 0x80) != 0
      val opcode = firstByte & 0x0F
      val masked = (secondByte & 0x80) != 0
      var payloadLength = secondByte & 0x7F

      // Handle extended payload length
      if (payloadLength == 126) {
        val lengthBuffer = ByteBuffer.allocate(2)
        sslRead(lengthBuffer)
        lengthBuffer.flip()
        payloadLength = lengthBuffer.getShort() & 0xFFFF
      } else if (payloadLength == 127) {
        val lengthBuffer = ByteBuffer.allocate(8)
        sslRead(lengthBuffer)
        lengthBuffer.flip()
        payloadLength = lengthBuffer.getLong().toInt // Simplified, should handle long properly
      }

      // Read payload
      val payloadBuffer = ByteBuffer.allocate(payloadLength)
      sslRead(payloadBuffer)
      val payload = payloadBuffer.array()

      opcode match {
        case 0x1 => // Text frame
          val message = new String(payload, "UTF-8")
          messageQueue.offer(message)
          owner.foreach(_.node.sendToVisible("computer.signal", "websocket_message", id.toString))
        case 0x2 => // Binary frame
          binaryQueue.offer(payload)
          owner.foreach(_.node.sendToVisible("computer.signal", "websocket_binary", id.toString))
        case 0x8 => // Close frame
          close()
        case 0x9 => // Ping frame
          sendWebSocketFrame(payload, isText = false, opcode = 0xA) // Send pong
        case 0xA => // Pong frame
          // Handle pong if needed
        case _ => // Unknown frame type, ignore
      }
    }

    private def sendWebSocketFrame(data: Any, isText: Boolean, opcode: Int = -1): Unit = {
      val payload = data match {
        case s: String => s.getBytes("UTF-8")
        case b: Array[Byte] => b
        case _ => throw new IllegalArgumentException("Invalid data type")
      }

      val actualOpcode = if (opcode != -1) opcode else if (isText) 0x1 else 0x2
      val frame = ByteBuffer.allocate(payload.length + 10) // Max header size

      // First byte: FIN + opcode
      frame.put((0x80 | actualOpcode).toByte)

      // Payload length
      if (payload.length < 126) {
        frame.put(payload.length.toByte)
      } else if (payload.length < 65536) {
        frame.put(126.toByte)
        frame.putShort(payload.length.toShort)
      } else {
        frame.put(127.toByte)
        frame.putLong(payload.length.toLong)
      }

      // Payload
      frame.put(payload)
      frame.flip()

      sslWrite(frame)
    }

    private class WebSocketConnector extends Callable[SocketChannel] {
      override def call(): SocketChannel = {
        checkLists(InetAddress.getByName(url.getHost), url.getHost)
        val port = if (url.getPort != -1) url.getPort else if (isSecure) 443 else 80
        val address = new InetSocketAddress(url.getHost, port)

        val socketChannel = SocketChannel.open()
        socketChannel.configureBlocking(true) // Use blocking for simplicity
        socketChannel.connect(address)

        socketChannel
      }
    }
  }

}
