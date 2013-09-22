package li.cil.oc.server.components

import li.cil.oc.api.{INetworkNode, INetworkMessage}
import net.minecraft.nbt.NBTTagCompound

/**
 * Graphics cards are what we use to render text to screens. 
 */
class GraphicsCard(nbt: NBTTagCompound) extends INetworkNode {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  override def receive(message: INetworkMessage) = message.data match {
    case Array(screen: Int, w: Int, h: Int) if message.name == "gpu.setResolution" =>
      if (supportedResolutions.contains((w, h)))
        network.sendToNode(message.source, screen, "screen.setResolution", w, h)
    case Array(screen: Int) if message.name == "gpu.getResolution" =>
      network.sendToNode(message.source, screen, "screen.getResolution")
    case Array(screen: Int) if message.name == "gpu.getResolutions" =>
      network.sendToNode(this, screen, "screen.getResolutions")
    case Array(resolutions @ _*) if message.name == "screen.resolutions" =>
      // TODO how to know to which computer to return this result? pass along to screen? pass our resolutions along and send result to computer directly?
      network.sendToNode(this, ???, "gpu.resolutions", supportedResolutions.intersect(resolutions): _*)
    case Array(screen: Int, x: Int, y: Int, value: String) if message.name == "gpu.set" =>
      network.sendToNode(this, screen, "screen.set", x - 1, y - 1, value)
    case Array(screen: Int, x: Int, y: Int, w: Int, h: Int, value: String) if message.name == "gpu.fill" =>
      if (value != null && value.length == 1)
        network.sendToNode(this, screen, "screen.fill", x - 1, y - 1, w, h, value.charAt(0))
    case Array(screen: Int, x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) if message.name == "gpu.copy" =>
      network.sendToNode(this, screen, "screen.copy", x - 1, y - 1, w, h, tx, ty)
  }
}