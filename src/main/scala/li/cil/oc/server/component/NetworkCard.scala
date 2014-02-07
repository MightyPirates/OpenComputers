package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class NetworkCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    create()

  protected val openPorts = mutable.Set.empty[Int]

  // ----------------------------------------------------------------------- //

  @Callback
  def open(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.add(port))
  }

  @Callback
  def close(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count == 0) {
      openPorts.clear()
      result(true)
    }
    else {
      val port = checkPort(args.checkInteger(0))
      result(openPorts.remove(port))
    }
  }

  @Callback(direct = true)
  def isOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.contains(port))
  }

  @Callback(direct = true)
  def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(false)

  @Callback
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    checkPacketSize(args.drop(2))
    node.sendToAddress(address, "network.message", Seq(Int.box(port)) ++ args.drop(2): _*)
    result(true)
  }

  @Callback
  def broadcast(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    checkPacketSize(args.drop(1))
    node.sendToReachable("network.message", Seq(Int.box(port)) ++ args.drop(1): _*)
    result(true)
  }

  @Callback(direct = true)
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
      case Array(port: Integer, args@_*) if openPorts.contains(port) =>
        node.sendToReachable("computer.signal", Seq("modem_message", message.source.address, Int.box(port), Int.box(0)) ++ args: _*)
      case _ =>
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
