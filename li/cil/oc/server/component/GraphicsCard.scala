package li.cil.oc.server.component

import li.cil.oc.api.network.{Node, Visibility, Message}
import li.cil.oc.common.component.ScreenEnvironment
import net.minecraft.nbt.NBTTagCompound

class GraphicsCard extends Node {
  val supportedResolutions = List(List(40, 24), List(80, 24))

  private var screen: Option[String] = None

  // ----------------------------------------------------------------------- //

  override def name = "gpu"

  override def visibility = Visibility.Neighbors

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array(address: Array[Byte]) if message.name == "gpu.bind" =>
        network.fold(None: Option[Array[Any]])(network => {
          network.node(new String(address, "UTF-8")) match {
            case None => result(Unit, "invalid address")
            case Some(node: ScreenEnvironment) =>
              screen = node.address
              result(true)
            case _ => result(Unit, "not a screen")
          }
        })
      case Array() if message.name == "network.disconnect" && message.source.address == screen => screen = None; None
      case Array(w: Double, h: Double) if message.name == "gpu.resolution=" =>
        if (supportedResolutions.contains((w.toInt, h.toInt)))
          trySend("screen.resolution=", w.toInt, h.toInt)
        else
          result(Unit, "unsupported resolution")
      case Array() if message.name == "gpu.resolution" => trySend("screen.resolution")
      case Array() if message.name == "gpu.resolutions" => trySend("screen.resolutions") match {
        case Some(Array(resolutions@_*)) =>
          result(supportedResolutions.intersect(resolutions): _*)
        case _ => None
      }
      case Array(x: Double, y: Double, value: Array[Byte]) if message.name == "gpu.set" =>
        trySend("screen.set", x.toInt - 1, y.toInt - 1, new String(value, "UTF-8"))
      case Array(x: Double, y: Double, w: Double, h: Double, value: Array[Byte]) if message.name == "gpu.fill" =>
        val s = new String(value, "UTF-8")
        if (s.length == 1)
          trySend("screen.fill", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, s.charAt(0))
        else
          result(Unit, "invalid fill value")
      case Array(x: Double, y: Double, w: Double, h: Double, tx: Double, ty: Double) if message.name == "gpu.copy" =>
        trySend("screen.copy", x.toInt - 1, y.toInt - 1, w.toInt, h.toInt, tx.toInt, ty.toInt)
      case _ => None
    }
  }

  override protected def onDisconnect() = {
    super.onDisconnect()
    screen = None
  }

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    if (nbt.hasKey("screen"))
      screen = Some(nbt.getString("screen"))
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)
    if (screen.isDefined)
      nbt.setString("screen", screen.get)
  }

  // ----------------------------------------------------------------------- //

  private def trySend(name: String, data: Any*): Option[Array[Any]] =
    screen match {
      case None => result(Unit, "no screen")
      case Some(screenAddress) => network.fold(None: Option[Array[Any]])(net => {
        net.sendToAddress(this, screenAddress, name, data: _*)
      })
    }
}