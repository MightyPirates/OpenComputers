package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.api.network.environment.LuaCallback
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class NetworkCard extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "network", Visibility.Network))
  node.visibility(Visibility.Neighbors)

  private val openPorts = mutable.Set.empty[Int]

  // ----------------------------------------------------------------------- //

  @LuaCallback("open")
  def open(message: Message): Array[Object] = {
    val port = checkPort(message.checkInteger(1))
    result(openPorts.add(port))
  }

  @LuaCallback("close")
  def close(message: Message): Array[Object] = {
    if (message.data.length < 2) {
      openPorts.clear()
      result(true)
    }
    else {
      val port = checkPort(message.checkInteger(1))
      result(openPorts.remove(port))
    }
  }

  @LuaCallback("isOpen")
  def isOpen(message: Message): Array[Object] = {
    val port = checkPort(message.checkInteger(1))
    result(openPorts.contains(port))
  }

  @LuaCallback("send")
  def send(message: Message): Array[Object] = {
    val address = message.checkString(1)
    val port = checkPort(message.checkInteger(2))
    node.network.sendToAddress(node, address, "network.message", Seq(Int.box(port)) ++ message.data.drop(3): _*)
    result(true)
  }

  @LuaCallback("broadcast")
  def broadcast(message: Message): Array[Object] = {
    val port = checkPort(message.checkInteger(1))
    node.network.sendToVisible(node, "network.message", Seq(Int.box(port)) ++ message.data.drop(2): _*)
    result(true)
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    message.data match {
      case Array() if message.name == "computer.stopped" =>
        if (node.network.neighbors(message.source).exists(_ == this)) openPorts.clear()
      case Array(port: Integer, args@_*) if message.name == "network.message" =>
        if (openPorts.contains(port))
          node.network.sendToNeighbors(node, "computer.signal", Seq("network_message", message.source.address, port) ++ args: _*)
      case _ =>
    }
    null
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    if (nbt.hasKey("oc.net.openPorts")) {
      val openPortsNbt = nbt.getTagList("oc.net.openPorts")
      (0 until openPortsNbt.tagCount).
        map(openPortsNbt.tagAt).
        map(_.asInstanceOf[NBTTagInt]).
        foreach(portNbt => openPorts.add(portNbt.data))
    }
  }

  override def save(nbt: NBTTagCompound) {
    val openPortsNbt = new NBTTagList()
    for (port <- openPorts)
      openPortsNbt.appendTag(new NBTTagInt(null, port))
    nbt.setTag("oc.net.openPorts", openPortsNbt)
  }

  // ----------------------------------------------------------------------- //

  private def checkPort(port: Int) =
    if (port < 1 || port > 0xFFFF) throw new IllegalArgumentException("invalid port number")
    else port
}
