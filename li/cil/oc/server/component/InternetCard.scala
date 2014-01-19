package li.cil.oc.server.component

import java.io.{OutputStreamWriter, BufferedWriter, FileNotFoundException}
import java.net.{SocketTimeoutException, HttpURLConnection, URL}
import java.util.regex.Matcher
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ThreadPoolFactory
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import scala.Array
import scala.collection.mutable

class InternetCard extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("internet", Visibility.Neighbors).
    create()

  // node address -> list per request -> list of signals as (request url, packets)
  private val queues = mutable.Map.empty[String, mutable.Queue[(String, mutable.Queue[Array[Byte]])]]

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "isHttpEnabled", direct = true)
  def isHttpEnabled(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.httpEnabled)

  @LuaCallback("request")
  def request(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    if (!Settings.get.httpEnabled) return result(false, "http requests are unavailable")
    checkAddress(address)
    val post = if (args.isString(1)) Option(args.checkString(1)) else None
    InternetCard.threadPool.submit(new Runnable {
      def run() = try {
        val proxy = Option(MinecraftServer.getServer.getServerProxy).getOrElse(java.net.Proxy.NO_PROXY)
        val url = new URL(address)
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

  private def checkAddress(address: String) {
    val url = try new URL(address)
    catch {
      case e: Throwable => throw new FileNotFoundException("invalid address")
    }
    val protocol = url.getProtocol
    if (!protocol.matches("^https?$")) {
      throw new FileNotFoundException("unsupported protocol")
    }
    val host = Matcher.quoteReplacement(url.getHost)
    if (Settings.get.httpHostWhitelist.length > 0 && !Settings.get.httpHostWhitelist.exists(host.matches)) {
      throw new FileNotFoundException("domain is not whitelisted")
    }
    if (Settings.get.httpHostBlacklist.exists(host.matches)) {
      throw new FileNotFoundException("domain is blacklisted")
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
}

object InternetCard {
  private val threadPool = ThreadPoolFactory.create("HTTP", Settings.get.httpThreads)
}