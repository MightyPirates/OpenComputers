package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.Some

class NetworkCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    create()

  protected val openPorts = mutable.Set.empty[Int]

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(port:number):boolean -- Opens the specified port. Returns true if the port was opened.""")
  def open(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.add(port))
  }

  @Callback(doc = """function([port:number]):boolean -- Closes the specified port (default: all ports). Returns true if ports were closed.""")
  def close(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count == 0) {
      val closed = openPorts.size > 0
      openPorts.clear()
      result(closed)
    }
    else {
      val port = checkPort(args.checkInteger(0))
      result(openPorts.remove(port))
    }
  }

  @Callback(direct = true, doc = """function(port:number):boolean -- Whether the specified port is open.""")
  def isOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.contains(port))
  }

  @Callback(direct = true, doc = """function():boolean -- Whether this is a wireless network card.""")
  def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(false)

  @Callback(doc = """function(address:string, port:number, data...) -- Sends the specified data to the specified target.""")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    checkPacketSize(args.drop(2))
    node.sendToReachable("network.message", new NetworkCard.Packet(node.address, Some(address), port, args.drop(2)))
    result(true)
  }

  @Callback(doc = """function(port:number, data...) -- Broadcasts the specified data on the specified port.""")
  def broadcast(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    checkPacketSize(args.drop(1))
    node.sendToReachable("network.message", new NetworkCard.Packet(node.address, None, port, args.drop(1)))
    result(true)
  }

  @Callback(direct = true, doc = """function():number -- Gets the maximum packet size (config setting).""")
  def maxPacketSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.maxNetworkPacketSize)

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      openPorts.clear()
    }
  }

  override def onMessage(message: Message) = {
    super.onMessage(message)
    if ((message.name == "computer.stopped" || message.name == "computer.started") && node.isNeighborOf(message.source))
      openPorts.clear()
    if (message.name == "network.message") message.data match {
      case Array(packet: NetworkCard.Packet) if packet.source != node.address && packet.dest.forall(_ == node.address) && openPorts.contains(packet.port) =>
        node.sendToReachable("computer.signal", Seq("modem_message", packet.source, Int.box(packet.port), Int.box(0)) ++ packet.data: _*)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    assert(openPorts.isEmpty)
    openPorts ++= nbt.getTagList("openPorts").iterator[NBTTagInt].map(_.data)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    nbt.setNewTagList("openPorts", openPorts)
  }

  // ----------------------------------------------------------------------- //

  protected def checkPort(port: Int) =
    if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number")
    else port

  protected def checkPacketSize(data: Iterable[AnyRef]) {
    val size = data.foldLeft(0)((acc, arg) => {
      acc + (arg match {
        case null | Unit | None => 4
        case _: java.lang.Boolean => 4
        case _: java.lang.Double => 8
        case value: java.lang.String => value.length
        case value: Array[Byte] => value.length
      })
    })
    if (size > Settings.get.maxNetworkPacketSize) {
      throw new IllegalArgumentException("packet too big (max " + Settings.get.maxNetworkPacketSize + ")")
    }
  }
}

object NetworkCard {
  class Packet(val source: String, val dest: Option[String], val port: Int, val data: Iterable[AnyRef], val ttl: Int = 5) {
    def hop() = new Packet(source, dest, port, data, ttl - 1)

    def save(nbt: NBTTagCompound) {
      nbt.setString("source", source)
      nbt.setBoolean("broadcast", dest.isEmpty)
      dest.foreach(nbt.setString("dest", _))
      nbt.setInteger("port", port)
      nbt.setInteger("ttl", ttl)
      val dataArray = data.toArray
      nbt.setInteger("dataLength", dataArray.length)
      for (i <- 0 until dataArray.length) dataArray(i) match {
        case null | Unit | None =>
        case value: java.lang.Boolean => nbt.setBoolean("data" + i, value)
        case value: java.lang.Double => nbt.setDouble("data" + i, value)
        case value: java.lang.String => nbt.setString("data" + i, value)
        case value: Array[Byte] => nbt.setByteArray("data" + i, value)
      }
    }
  }

  def loadPacket(nbt: NBTTagCompound) = {
    val source = nbt.getString("source")
    val dest =
      if (nbt.getBoolean("broadcast")) None
      else Option(nbt.getString("dest"))
    val port = nbt.getInteger("port")
    val ttl = nbt.getInteger("ttl")
    val data = for (i <- 0 until nbt.getInteger("dataLength")) yield {
      if (nbt.hasKey("data" + i)) {
        nbt.getTag("data" + i) match {
          case boolean: NBTTagByte => Boolean.box(boolean.data == 1)
          case double: NBTTagDouble => Double.box(double.data)
          case string: NBTTagString => string.data: AnyRef
          case array: NBTTagByteArray => array.byteArray
        }
      }
      else null
    }
    new Packet(source, dest, port, data, ttl)
  }
}