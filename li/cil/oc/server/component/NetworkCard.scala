package li.cil.oc.server.component

import li.cil.oc.api.network.{Message, Visibility, Node}
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.collection.mutable

class NetworkCard extends Node {
  private val openPorts = mutable.Set.empty[Int]

  override val name = "network"

  override val visibility = Visibility.Network

  override lazy val computerVisibility = Visibility.Neighbors

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array() if message.name == "computer.stopped" =>
        if (network.get.neighbors(message.source).exists(_ == this))
          openPorts.clear()
        None

      case Array(port: Double) if message.name == "network.open=" =>
        if (isPortValid(port.toInt)) result(openPorts.add(port.toInt))
        else result(Unit, "invalid port number")
      case Array(port: Double) if message.name == "network.open" =>
        if (isPortValid(port.toInt)) result(openPorts.contains(port.toInt))
        else result(Unit, "invalid port number")
      case Array(port: Double) if message.name == "network.close" =>
        if (isPortValid(port.toInt)) result(openPorts.remove(port.toInt))
        else result(Unit, "invalid port number")
      case Array() if message.name == "network.close" =>
        openPorts.clear()
        result(true)
      case Array(address: Array[Byte], port: Double, args@_*) if message.name == "network.send" =>
        if (isPortValid(port.toInt))
          network.get.sendToAddress(this, new String(address, "UTF-8"), "network.message", Seq(port.toInt) ++ args: _*)
        None
      case Array(port: Double, args@_*) if message.name == "network.broadcast" => None
        if (isPortValid(port.toInt))
          network.get.sendToVisible(this, "network.message", Seq(port.toInt) ++ args: _*)
        None

      case Array(port: Int, args@_*) if message.name == "network.message" =>
        if (openPorts.contains(port))
          network.get.sendToNeighbors(this, "computer.signal", Seq("network_message", message.source.address.get, port) ++ args: _*)
        None
      case _ => None // Ignore.
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("openPorts")) {
      val openPortsNbt = nbt.getTagList("openPorts")
      (0 until openPortsNbt.tagCount).
        map(openPortsNbt.tagAt).
        map(_.asInstanceOf[NBTTagInt]).
        foreach(portNbt => openPorts.add(portNbt.data))
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    val openPortsNbt = new NBTTagList()
    for (port <- openPorts)
      openPortsNbt.appendTag(new NBTTagInt(null, port))
    nbt.setTag("openPorts", openPortsNbt)
  }

  private def isPortValid(port: Int) = port >= 1 && port <= 0xFFFF
}
