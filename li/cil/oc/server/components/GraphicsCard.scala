package li.cil.oc.server.components

import li.cil.oc.api.{INetworkNode, INetworkMessage}
import net.minecraft.nbt.NBTTagCompound

class GraphicsCard(nbt: NBTTagCompound) extends INetworkNode {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  override def name = "gpu"

  override def receive(message: INetworkMessage) = {
    // We don't need onConnect / onDisconnect / onAddressChange yet, so no need to call super.
    // super.receive(message)
    message.data match {
      case Array(screen: Double, w: Double, h: Double) if message.name == "gpu.resolution=" =>
        if (supportedResolutions.contains((w.toInt, h.toInt)))
          network.sendToNode(message.source, screen.toInt, "screen.resolution=", w.toInt, h.toInt)
        else Some(Array(None, "unsupported resolution"))
      case Array(screen: Double) if message.name == "gpu.resolution" =>
        network.sendToNode(message.source, screen.toInt, "screen.resolution")
      case Array(screen: Double) if message.name == "gpu.resolutions" =>
        network.sendToNode(this, screen.toInt, "screen.resolutions") match {
          case Some(Array(resolutions@_*)) =>
            Some(Array(supportedResolutions.intersect(resolutions): _*))
          case _ => None
        }
      case Array(screen: Double, x: Double, y: Double, value: String) if message.name == "gpu.set" =>
        network.sendToNode(this, screen.toInt, "screen.set", x.toInt - 1, y.toInt - 1, value)
      case Array(screen: Double, x: Double, y: Double, w: Double, h: Double, value: String) if message.name == "gpu.fill" =>
        if (value != null && value.length == 1)
          network.sendToNode(this, screen.toInt, "screen.fill", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, value.charAt(0))
        else Some(Array(None, "invalid fill value"))
      case Array(screen: Double, x: Double, y: Double, w: Double, h: Double, tx: Double, ty: Double) if message.name == "gpu.copy" =>
        network.sendToNode(this, screen.toInt, "screen.copy", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, tx.toInt, ty.toInt)
      case _ => None
    }
  }
}