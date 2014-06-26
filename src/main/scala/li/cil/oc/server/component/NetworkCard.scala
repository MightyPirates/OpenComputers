package li.cil.oc.server.component

import li.cil.oc.{Settings, api}
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component
import net.minecraft.nbt._

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class NetworkCard extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    create()

  protected val openPorts = mutable.Set.empty[Int]

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(port:number):boolean -- Opens the specified port. Returns true if the port was opened.""")
  def open(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    if (openPorts.contains(port)) result(false)
    else if (openPorts.size >= Settings.get.maxOpenPorts) {
      throw new java.io.IOException("too many open ports")
    }
    else result(openPorts.add(port))
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
    val packet = api.Network.newPacket(node.address, address, port, args.drop(2).toArray)
    doSend(packet)
    result(true)
  }

  @Callback(doc = """function(port:number, data...) -- Broadcasts the specified data on the specified port.""")
  def broadcast(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    val packet = api.Network.newPacket(node.address, null, port, args.drop(1).toArray)
    doBroadcast(packet)
    result(true)
  }

  @Callback(direct = true, doc = """function():number -- Gets the maximum packet size (config setting).""")
  def maxPacketSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.maxNetworkPacketSize)

  protected def doSend(packet: Packet) {
    node.sendToReachable("network.message", packet)
  }

  protected def doBroadcast(packet: Packet) {
    node.sendToReachable("network.message", packet)
  }

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
      case Array(packet: Packet) => receivePacket(packet, 0)
      case _ =>
    }
  }

  def receivePacket(packet: Packet, distance: Double) {
    if (packet.source != node.address && Option(packet.destination).forall(_ == node.address) && openPorts.contains(packet.port)) {
      node.sendToReachable("computer.signal", Seq("modem_message", packet.source, Int.box(packet.port), Double.box(distance)) ++ packet.data: _*)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    assert(openPorts.isEmpty)
    openPorts ++= nbt.getIntArray("openPorts")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    nbt.setIntArray("openPorts", openPorts.toArray)
  }

  // ----------------------------------------------------------------------- //

  protected def checkPort(port: Int) =
    if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number")
    else port
}
