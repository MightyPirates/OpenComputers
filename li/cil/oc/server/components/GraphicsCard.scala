package li.cil.oc.server.components

import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.server.computer.NetworkNode
import li.cil.oc.api.INetworkMessage

/**
 * Graphics cards are what we use to render text to screens. 
 */
class GraphicsCard(nbt: NBTTagCompound) extends NetworkNode {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  override def receive(message: INetworkMessage) = message.getData match {
    case Array(screen: Int, w: Int, h: Int) if message.getName == "gpu.setResolution" =>
      if (supportedResolutions.contains((w, h)))
        getNetwork.sendToNode(message.getSource, screen, "screen.setResolution", w, h)
    case Array(screen: Int) if message.getName == "gpu.getResolution" =>
      getNetwork.sendToNode(message.getSource, screen, "screen.getResolution")
    case Array(screen: Int) if message.getName == "gpu.getResolutions" =>
      getNetwork.sendToNode(this, screen, "screen.getResolutions")
    case resolutions: Array[(Int, Int)] if message.getName == "screen.resolutions" =>
      // TODO how to know to which computer to return this result? pass along to screen? pass our resolutions along and send result to computer directly?
      getNetwork.sendToNode(this, ???, "gpu.resolutions", supportedResolutions.intersect(resolutions): _*)
    case Array(screen: Int, x: Int, y: Int, value: String) if message.getName == "gpu.set" =>
      getNetwork.sendToNode(this, screen, "screen.set", x - 1, y - 1, value)
    case Array(screen: Int, x: Int, y: Int, w: Int, h: Int, value: String) if message.getName == "gpu.fill" =>
      if (value != null && value.length == 1)
        getNetwork.sendToNode(this, screen, "screen.fill", x - 1, y - 1, w, h, value.charAt(0))
    case Array(screen: Int, x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) if message.getName == "gpu.copy" =>
      getNetwork.sendToNode(this, screen, "screen.copy", x - 1, y - 1, w, h, tx, ty)
  }
}