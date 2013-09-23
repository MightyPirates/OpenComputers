package li.cil.oc.server.components

import li.cil.oc.api.{INetworkNode, INetworkMessage}
import net.minecraft.nbt.NBTTagCompound

class GraphicsCard(nbt: NBTTagCompound) extends INetworkNode {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  override def receive(message: INetworkMessage) = message.data match {
    case Array(screen: Int, w: Int, h: Int) if message.name == "gpu.resolution=" =>
      if (supportedResolutions.contains((w, h)))
        network.sendToNode(message.source, screen, "screen.resolution=", w, h)
      else Some(Array(None, "unsupported resolution"))
    case Array(screen: Int) if message.name == "gpu.resolution" =>
      network.sendToNode(message.source, screen, "screen.resolution")
    case Array(screen: Int) if message.name == "gpu.resolutions" =>
      network.sendToNode(this, screen, "screen.resolutions") match {
        case Some(Array(resolutions@_*)) =>
          Some(Array(supportedResolutions.intersect(resolutions): _*))
        case _ => None
      }
    case Array(screen: Int, x: Int, y: Int, value: String) if message.name == "gpu.set" =>
      network.sendToNode(this, screen, "screen.set", x - 1, y - 1, value)
    case Array(screen: Int, x: Int, y: Int, w: Int, h: Int, value: String) if message.name == "gpu.fill" =>
      if (value != null && value.length == 1)
        network.sendToNode(this, screen, "screen.fill", x - 1, y - 1, w, h, value.charAt(0))
      else Some(Array(None, "invalid fill value"))
    case Array(screen: Int, x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) if message.name == "gpu.copy" =>
      network.sendToNode(this, screen, "screen.copy", x - 1, y - 1, w, h, tx, ty)
    case _ => None
  }
}