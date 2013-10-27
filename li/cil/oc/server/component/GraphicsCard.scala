package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.api.network.environment.LuaCallback
import net.minecraft.nbt.NBTTagCompound
import scala.Some

class GraphicsCard(val maxResolution: (Int, Int)) extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "gpu", Visibility.Neighbors))

  private var screen: Option[String] = None

  // ----------------------------------------------------------------------- //

  @LuaCallback("bind")
  def bind(message: Message): Array[Object] = {
    val address = message.checkString(1)
    node.network.node(address) match {
      case null => Array(Unit, "invalid address")
      case value if value.name() == "screen" =>
        screen = Option(address)
        result(true)
      case _ => Array(Unit, "not a screen")
    }
  }

  @LuaCallback("getResolution")
  def getResolution(message: Message): Array[Object] = trySend("screen.resolution")

  @LuaCallback("setResolution")
  def setResolution(message: Message): Array[Object] = {
    val w = message.checkInteger(1)
    val h = message.checkInteger(2)
    val (mw, mh) = maxResolution
    if (w <= mw && h <= mh)
      trySend("screen.resolution=", w, h)
    else
      Array(Unit, "unsupported resolution")
  }

  @LuaCallback("maxResolution")
  def maxResolution(message: Message): Array[Object] =
    trySend("screen.resolution") match {
      case Array(w: Integer, h: Integer) =>
        val (mw, mh) = maxResolution
        result(mw min w, mh min h)
      case _ => null
    }

  @LuaCallback("set")
  def set(message: Message): Array[Object] = {
    val x = message.checkInteger(1)
    val y = message.checkInteger(2)
    val value = message.checkString(3)
    trySend("screen.set", x - 1, y - 1, value)
  }

  @LuaCallback("fill")
  def fill(message: Message): Array[Object] = {
    val x = message.checkInteger(1)
    val y = message.checkInteger(2)
    val w = message.checkInteger(3)
    val h = message.checkInteger(4)
    val value = message.checkString(4)
    if (value.length == 1)
      trySend("screen.fill", x - 1, y - 1, w, h, value.charAt(0))
    else
      Array(Unit, "invalid fill value")
  }

  @LuaCallback("copy")
  def copy(message: Message): Array[Object] = {
    val x = message.checkInteger(1)
    val y = message.checkInteger(2)
    val w = message.checkInteger(3)
    val h = message.checkInteger(4)
    val tx = message.checkInteger(5)
    val ty = message.checkInteger(6)
    trySend("screen.copy", x - 1, y - 1, w, h, tx, ty)
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    if (message.name == "system.disconnect" && message.source.address == screen.orNull) {
      screen = None
    }
    null
  }

  override def onDisconnect() {
    screen = None
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("oc.gpu.screen"))
      screen = Some(nbt.getString("oc.gpu.screen"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    if (screen.isDefined)
      nbt.setString("oc.gpu.screen", screen.get)
  }

  // ----------------------------------------------------------------------- //

  private def trySend(name: String, data: Any*): Array[Object] =
    screen match {
      case Some(address) => node.network.sendToAddress(node, address, name, data.map(_.asInstanceOf[Object]): _*)
      case None => Array(Unit, "no screen")
    }
}