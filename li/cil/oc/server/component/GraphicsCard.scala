package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.Message

class GraphicsCard extends ItemComponent {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  override def name = "gpu"

  override protected def receiveFromNeighbor(network: Network, message: Message) = message.data match {
    case Array(screen: Array[Byte], w: Double, h: Double) if message.name == "gpu.resolution=" =>
      if (supportedResolutions.contains((w.toInt, h.toInt)))
        network.sendToAddress(this, new String(screen, "UTF-8"), "screen.resolution=", w.toInt, h.toInt)
      else
        Some(Array(Unit, "unsupported resolution"))
    case Array(screen: Array[Byte]) if message.name == "gpu.resolution" =>
      network.sendToAddress(this, new String(screen, "UTF-8"), "screen.resolution")
    case Array(screen: Array[Byte]) if message.name == "gpu.resolutions" =>
      network.sendToAddress(this, new String(screen, "UTF-8"), "screen.resolutions") match {
        case Some(Array(resolutions@_*)) =>
          Some(Array(supportedResolutions.intersect(resolutions): _*))
        case _ => None
      }
    case Array(screen: Array[Byte], x: Double, y: Double, value: Array[Byte]) if message.name == "gpu.set" =>
      network.sendToAddress(this, new String(screen, "UTF-8"), "screen.set", x.toInt - 1, y.toInt - 1, new String(value, "UTF-8"))
    case Array(screen: Array[Byte], x: Double, y: Double, w: Double, h: Double, value: Array[Byte]) if message.name == "gpu.fill" =>
      val s = new String(value, "UTF-8")
      if (s.length == 1)
        network.sendToAddress(this, new String(screen, "UTF-8"), "screen.fill", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, s.charAt(0))
      else
        Some(Array(Unit, "invalid fill value"))
    case Array(screen: Array[Byte], x: Double, y: Double, w: Double, h: Double, tx: Double, ty: Double) if message.name == "gpu.copy" =>
      network.sendToAddress(this, new String(screen, "UTF-8"), "screen.copy", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, tx.toInt, ty.toInt)
    case _ => None
  }
}