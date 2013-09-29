package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.Message
import net.minecraft.nbt.NBTTagCompound

class GraphicsCard(nbt: NBTTagCompound) extends ItemComponent(nbt) {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  override def name = "gpu"

  override def receive(message: Message) = {
    super.receive(message)
    def trySend(f: Network => Option[Array[Any]]) = network.fold(None: Option[Array[Any]])(f)
    message.data match {
      case Array(screen: Double, w: Double, h: Double) if message.name == "gpu.resolution=" =>
        if (supportedResolutions.contains((w.toInt, h.toInt)))
          trySend(_.sendToAddress(this, screen.toInt, "screen.resolution=", w.toInt, h.toInt))
        else Some(Array(None, "unsupported resolution"))
      case Array(screen: Double) if message.name == "gpu.resolution" =>
        trySend(_.sendToAddress(this, screen.toInt, "screen.resolution"))
      case Array(screen: Double) if message.name == "gpu.resolutions" =>
        trySend(_.sendToAddress(this, screen.toInt, "screen.resolutions")) match {
          case Some(Array(resolutions@_*)) =>
            Some(Array(supportedResolutions.intersect(resolutions): _*))
          case _ => None
        }
      case Array(screen: Double, x: Double, y: Double, value: String) if message.name == "gpu.set" =>
        trySend(_.sendToAddress(this, screen.toInt, "screen.set", x.toInt - 1, y.toInt - 1, value))
      case Array(screen: Double, x: Double, y: Double, w: Double, h: Double, value: String) if message.name == "gpu.fill" =>
        if (value != null && value.length == 1)
          trySend(_.sendToAddress(this, screen.toInt, "screen.fill", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, value.charAt(0)))
        else Some(Array(None, "invalid fill value"))
      case Array(screen: Double, x: Double, y: Double, w: Double, h: Double, tx: Double, ty: Double) if message.name == "gpu.copy" =>
        trySend(_.sendToAddress(this, screen.toInt, "screen.copy", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, tx.toInt, ty.toInt))
      case _ => None
    }
  }
}