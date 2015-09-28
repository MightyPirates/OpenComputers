package li.cil.oc.server.component

import com.google.common.base.Charsets
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import net.minecraft.nbt._

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class NetworkCard(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    create()

  protected val openPorts = mutable.Set.empty[Int]

  protected var wakeMessage: Option[String] = None

  protected var wakeMessageFuzzy = false

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
      val closed = openPorts.nonEmpty
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

  @Callback(direct = true, doc = """function():string, boolean -- Get the current wake-up message.""")
  def getWakeMessage(context: Context, args: Arguments): Array[AnyRef] = result(wakeMessage.orNull, wakeMessageFuzzy)

  @Callback(doc = """function(message:string[, fuzzy:boolean]):string -- Set the wake-up message and whether to ignore additional data/parameters.""")
  def setWakeMessage(context: Context, args: Arguments): Array[AnyRef] = {
    val oldMessage = wakeMessage
    val oldFuzzy = wakeMessageFuzzy

    if (args.optAny(0, null) == null)
      wakeMessage = None
    else
      wakeMessage = Option(args.checkString(0))
    wakeMessageFuzzy = args.optBoolean(1, wakeMessageFuzzy)

    result(oldMessage.orNull, oldFuzzy)
  }

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

  protected def receivePacket(packet: Packet, distance: Double) {
    if (packet.source != node.address && Option(packet.destination).forall(_ == node.address)) {
      if (openPorts.contains(packet.port)) {
        node.sendToReachable("computer.signal", Seq("modem_message", packet.source, Int.box(packet.port), Double.box(distance)) ++ packet.data: _*)
      }
      // Accept wake-up messages regardless of port because we close all ports
      // when our computer shuts down.
      packet.data match {
        case Array(message: Array[Byte]) if wakeMessage.contains(new String(message, Charsets.UTF_8)) =>
          node.sendToNeighbors("computer.start")
        case Array(message: String) if wakeMessage.contains(message) =>
          node.sendToNeighbors("computer.start")
        case Array(message: Array[Byte], _*) if wakeMessageFuzzy && wakeMessage.contains(new String(message, Charsets.UTF_8)) =>
          node.sendToNeighbors("computer.start")
        case Array(message: String, _*) if wakeMessageFuzzy && wakeMessage.contains(message) =>
          node.sendToNeighbors("computer.start")
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    assert(openPorts.isEmpty)
    openPorts ++= nbt.getIntArray("openPorts")
    if (nbt.hasKey("wakeMessage")) {
      wakeMessage = Option(nbt.getString("wakeMessage"))
    }
    wakeMessageFuzzy = nbt.getBoolean("wakeMessageFuzzy")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    nbt.setIntArray("openPorts", openPorts.toArray)
    wakeMessage.foreach(nbt.setString("wakeMessage", _))
    nbt.setBoolean("wakeMessageFuzzy", wakeMessageFuzzy)
  }

  // ----------------------------------------------------------------------- //

  protected def checkPort(port: Int) =
    if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number")
    else port
}
