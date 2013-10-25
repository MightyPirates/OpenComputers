package li.cil.oc.server.component

import li.cil.oc.api.network.{Component, Message, Visibility}
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.collection.mutable

class NetworkCard extends Component {
  private val openPorts = mutable.Set.empty[Int]

  override val name = "network"

  override val visibility = Visibility.Network

  componentVisibility = Visibility.Neighbors

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array() if message.name == "computer.stopped" =>
        if (network.get.neighbors(message.source).exists(_ == this))
          openPorts.clear()
        None

      case Array(port: java.lang.Double) if message.name == "network.open=" =>
        if (isPortValid(port.toInt)) result(openPorts.add(port.toInt))
        else result(Unit, "invalid port number")
      case Array(port: java.lang.Double) if message.name == "network.open" =>
        if (isPortValid(port.toInt)) result(openPorts.contains(port.toInt))
        else result(Unit, "invalid port number")
      case Array(port: java.lang.Double) if message.name == "network.close" =>
        if (isPortValid(port.toInt)) result(openPorts.remove(port.toInt))
        else result(Unit, "invalid port number")
      case Array() if message.name == "network.close" =>
        openPorts.clear()
        result(true)
      case Array(address: Array[Byte], port: java.lang.Double, args@_*) if message.name == "network.send" =>
        if (isPortValid(port.toInt))
          network.get.sendToAddress(this, new String(address, "UTF-8"), "network.message", Seq(Int.box(port.toInt)) ++ args: _*)
        None
      case Array(port: java.lang.Double, args@_*) if message.name == "network.broadcast" => None
        if (isPortValid(port.toInt))
          network.get.sendToVisible(this, "network.message", Seq(Int.box(port.toInt)) ++ args: _*)
        None

      case Array(port: Integer, args@_*) if message.name == "network.message" =>
        if (openPorts.contains(port))
          network.get.sendToNeighbors(this, "computer.signal", Seq("network_message", message.source.address.get, port) ++ args: _*)
        None
      case _ => None // Ignore.
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (nbt.hasKey("openPorts")) {
      val openPortsNbt = nbt.getTagList("openPorts")
      (0 until openPortsNbt.tagCount).
        map(openPortsNbt.tagAt).
        map(_.asInstanceOf[NBTTagInt]).
        foreach(portNbt => openPorts.add(portNbt.data))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    val openPortsNbt = new NBTTagList()
    for (port <- openPorts)
      openPortsNbt.appendTag(new NBTTagInt(null, port))
    nbt.setTag("openPorts", openPortsNbt)
  }

  private def isPortValid(port: Int) = port >= 1 && port <= 0xFFFF
}
