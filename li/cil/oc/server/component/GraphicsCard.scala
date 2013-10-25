package li.cil.oc.server.component

import li.cil.oc.api.network.{Component, Visibility, Message}
import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound

class GraphicsCard(val maxResolution: (Int, Int)) extends Component {
  private var screen: Option[String] = None

  override val name = "gpu"

  override val visibility = Visibility.Neighbors

  componentVisibility = visibility

  // ----------------------------------------------------------------------- //

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array(address: Array[Byte]) if message.name == "gpu.bind" =>
        network.fold(None: Option[Array[AnyRef]])(network => {
          network.node(new String(address, "UTF-8")) match {
            case None => result(Unit, "invalid address")
            case Some(node: component.Screen.Environment) =>
              screen = node.address
              result(true)
            case _ => result(Unit, "not a screen")
          }
        })
      case Array() if message.name == "system.disconnect" && message.source.address == screen => screen = None; None
      case Array(w: java.lang.Double, h: java.lang.Double) if message.name == "gpu.resolution=" =>
        val (mw, mh) = maxResolution
        if (w.toInt <= mw && h.toInt <= mh)
          trySend("screen.resolution=", Int.box(w.toInt), Int.box(h.toInt))
        else
          result(Unit, "unsupported resolution")
      case Array() if message.name == "gpu.resolution" => trySend("screen.resolution")
      case Array() if message.name == "gpu.maxResolution" => trySend("screen.maxResolution") match {
        case Some(Array(w: Integer, h: Integer)) =>
          val (mw, mh) = maxResolution
          result(w.toInt min mw, h.toInt min mh)
        case _ => None
      }
      case Array(x: java.lang.Double, y: java.lang.Double, value: Array[Byte]) if message.name == "gpu.set" =>
        trySend("screen.set", Int.box(x.toInt - 1), Int.box(y.toInt - 1), new String(value, "UTF-8"))
      case Array(x: java.lang.Double, y: java.lang.Double, w: java.lang.Double, h: java.lang.Double, value: Array[Byte]) if message.name == "gpu.fill" =>
        val s = new String(value, "UTF-8")
        if (s.length == 1)
          trySend("screen.fill", Int.box(x.toInt - 1), Int.box(y.toInt - 1), Int.box(w.toInt), Int.box(h.toInt), Char.box(s.charAt(0)))
        else
          result(Unit, "invalid fill value")
      case Array(x: java.lang.Double, y: java.lang.Double, w: java.lang.Double, h: java.lang.Double, tx: java.lang.Double, ty: java.lang.Double) if message.name == "gpu.copy" =>
        trySend("screen.copy", Int.box(x.toInt - 1), Int.box(y.toInt - 1), Int.box(w.toInt), Int.box(h.toInt), Int.box(tx.toInt), Int.box(ty.toInt))
      case _ => None
    }
  }

  override protected def onDisconnect() = {
    super.onDisconnect()
    screen = None
  }

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    if (nbt.hasKey("screen"))
      screen = Some(nbt.getString("screen"))
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    if (screen.isDefined)
      nbt.setString("screen", screen.get)
  }

  // ----------------------------------------------------------------------- //

  private def trySend(name: String, data: AnyRef*): Option[Array[AnyRef]] =
    screen match {
      case None => result(Unit, "no screen")
      case Some(screenAddress) => network.fold(None: Option[Array[AnyRef]])(net => {
        net.sendToAddress(this, screenAddress, name, data: _*)
      })
    }
}