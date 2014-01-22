package li.cil.oc.server.component

import java.io.{IOException, OutputStreamWriter, BufferedWriter, FileNotFoundException}
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.regex.Matcher
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import scala.Array
import scala.collection.mutable

class InternetCard(val owner: Context) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  protected val connections = mutable.Map.empty[Int, SocketChannel]

  // node address -> list per request -> list of signals as (request url, packets)
  protected val queues = mutable.Map.empty[String, mutable.Queue[(String, mutable.Queue[Array[Byte]])]]

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "isHttpEnabled", direct = true)
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @LuaCallback("request")
  def request(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    if (!Settings.get.httpEnabled) return result(false, "http requests are unavailable")
    val url = checkAddress(address)
    val post = if (args.isString(1)) Option(args.checkString(1)) else None
    InternetCard.threadPool.submit(new Runnable {
      def run() = try {
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
            queues.synchronized(queues.getOrElseUpdate(context.address, mutable.Queue.empty) += address -> (mutable.Queue(data.toArray.grouped(Settings.get.maxNetworkPacketSize).toSeq: _*) ++ Iterable(null)))
          }
          finally {
            http.disconnect()
          }
          case other => context.signal("http_response", address, Unit, "connection failed")
        }
      }
      catch {
        case e: FileNotFoundException =>
          context.signal("http_response", address, Unit, "not found: " + Option(e.getMessage).getOrElse(e.toString))
        case _: SocketTimeoutException =>
          context.signal("http_response", address, Unit, "timeout")
        case e: Throwable =>
          context.signal("http_response", address, Unit, Option(e.getMessage).getOrElse(e.toString))
      }
    })
    result(true)
  }

  @LuaCallback(value = "isTcpEnabled", direct = true)
  def isTcpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @LuaCallback(value = "connect")
  def connect(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    val port = if (args.count > 1) args.checkInteger(1) else -1
    if (!Settings.get.tcpEnabled) return result(false, "tcp connections are unavailable")
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

  @LuaCallback(value = "close")
  def close(context: Context, args: Arguments): Array[AnyRef] = {
    val handle = args.checkInteger(0)
    connections.remove(handle) match {
      case Some(socket) => socket.close()
      case _ => throw new IllegalArgumentException("bad connection descriptor")
    }
    null
  }

  @LuaCallback(value = "write")
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

  @LuaCallback(value = "read")
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

    queues.synchronized {
      for ((nodeAddress, queue) <- queues if queue.nonEmpty) {
        node.network.node(nodeAddress) match {
          case computer: Node =>
            computer.host match {
              case context: Context =>
                val (address, packets) = queue.front
                if (context.signal("http_response", address, packets.front)) {
                  packets.dequeue()
                }
              case _ => queue.clear()
            }
          case _ => queue.clear()
        }
        // Remove all responses that have no more packets (usually only the
        // first on when it has been processed).
        queue.dequeueAll(_._2.isEmpty)
      }
      // Remove all targets that have no more responses.
      queues.retain((_, queue) => queue.nonEmpty)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    super.onMessage(message)
    message.data match {
      case Array() if (message.name == "computer.stopped" || message.name == "computer.started") && message.source.address == owner.address =>
        connections.values.foreach(_.close())
        connections.clear()
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    queues.synchronized {
      queues.clear()
      if (nbt.hasKey("queues")) {
        queues ++= nbt.getTagList("queues").iterator[NBTTagCompound].map(nodeNbt => {
          val nodeAddress = nodeNbt.getString("nodeAddress")
          val responses = mutable.Queue(nodeNbt.getTagList("responses").iterator[NBTTagCompound].map(responseNbt => {
            val address = responseNbt.getString("address")
            val data = responseNbt.getByteArray("data")
            (address, mutable.Queue(data.grouped(Settings.get.maxNetworkPacketSize).toSeq: _*) ++ Iterable(null))
          }): _*)
          nodeAddress -> responses
        })
      }
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    queues.synchronized {
      if (!queues.isEmpty) {
        nbt.setNewTagList("queues", queues.toIterable.map(
          node => {
            val (nodeAddress, responses) = node
            val nodeNbt = new NBTTagCompound()
            nodeNbt.setString("nodeAddress", nodeAddress)
            nodeNbt.setNewTagList("responses", responses.toIterable.map(
              response => {
                val (address, packets) = response
                val responseNbt = new NBTTagCompound()
                responseNbt.setString("address", address)
                val data = mutable.ArrayBuffer.empty[Byte]
                packets.toIterable.dropRight(1).foreach(data.appendAll(_))
                responseNbt.setByteArray("data", data.toArray)
                responseNbt
              }
            ))
          }
        ))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private def checkUri(address: String) = {
    val parsed = new URI(address)
    if (parsed.getHost != null && parsed.getPort != -1) {
      checkLists(Matcher.quoteReplacement(parsed.getHost))
      parsed
    }
    else {
      val simple = new URI("protocol://" + address)
      if (simple.getHost != null && simple.getPort != -1) {
        checkLists(Matcher.quoteReplacement(simple.getHost))
        simple
      }
      else parsed
    }
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