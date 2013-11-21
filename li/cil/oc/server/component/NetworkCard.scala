package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.{NBTTagInt, NBTTagCompound}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class NetworkCard extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    create()

  protected val openPorts = mutable.Set.empty[Int]

  // ----------------------------------------------------------------------- //

  @LuaCallback("open")
  def open(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.add(port))
  }

  @LuaCallback("close")
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

  @LuaCallback(value = "isOpen", direct = true)
  def isOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    result(openPorts.contains(port))
  }

  @LuaCallback(value = "isWireless", direct = true)
  def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(false)

  @LuaCallback("send")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    node.sendToAddress(address, "network.message", Seq(Int.box(port)) ++ args.drop(2): _*)
    result(true)
  }

  @LuaCallback("broadcast")
  def broadcast(context: Context, args: Arguments): Array[AnyRef] = {
    val port = checkPort(args.checkInteger(0))
    node.sendToReachable("network.message", Seq(Int.box(port)) ++ args.drop(1): _*)
    result(true)
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
      case Array(port: Integer, args@_*) if openPorts.contains(port) =>
        node.sendToReachable("computer.signal", Seq("modem_message", message.source.address, Int.box(port)) ++ args: _*)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    assert(openPorts.isEmpty)
    openPorts ++ nbt.getTagList("openPorts").iterator[NBTTagInt].map(_.data)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    nbt.setNewTagList("openPorts", openPorts)
  }

  // ----------------------------------------------------------------------- //

  protected def checkPort(port: Int) =
    if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number")
    else port
}
