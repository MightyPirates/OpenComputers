package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.component.Screen
import net.minecraft.nbt.NBTTagCompound
import scala.Some

class GraphicsCard(val maxResolution: (Int, Int)) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("gpu").
    create()

  private var screenAddress: Option[String] = None

  private var screenInstance: Option[Screen] = None

  private def screen(f: (Screen) => Array[Object]) = {
    if (screenInstance.isEmpty && screenAddress.isDefined) {
      Option(node.network.node(screenAddress.get)) match {
        case Some(node: Node) if node.host.isInstanceOf[Screen.Environment] =>
          screenInstance = Some(node.host.asInstanceOf[Screen.Environment].instance)
        case _ =>
          // This could theoretically happen after loading an old address, but
          // if the screen either disappeared between saving and now or changed
          // type. The first scenario is more likely, and could happen if the
          // chunk the screen is in isn't loaded when the chunk the GPU is in
          // gets loaded.
          screenAddress = None
      }
    }
    screenInstance match {
      case Some(screen) => f(screen)
      case _ => Array(Unit, "no screen")
    }
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("bind")
  def bind(context: Context, args: Arguments): Array[Object] = {
    val address = args.checkString(0)
    node.network.node(address) match {
      case null => Array(Unit, "invalid address")
      case node: Node if node.host.isInstanceOf[Screen.Environment] =>
        screenAddress = Option(address)
        screenInstance = None
        result(true)
      case _ => Array(Unit, "not a screen")
    }
  }

  @LuaCallback(value = "getResolution", asynchronous = true)
  def getResolution(context: Context, args: Arguments): Array[Object] =
    screen(s => {
      val (w, h) = s.resolution
      result(w, h)
    })

  @LuaCallback("setResolution")
  def setResolution(context: Context, args: Arguments): Array[Object] = {
    val w = args.checkInteger(0)
    val h = args.checkInteger(1)
    val (mw, mh) = maxResolution
    if (w > 0 && h > 0 && w <= mw && h <= mh)
      screen(s => result(s.resolution = (w, h)))
    else
      Array(Unit, "unsupported resolution")
  }

  @LuaCallback(value = "maxResolution", asynchronous = true)
  def maxResolution(context: Context, args: Arguments): Array[Object] =
    screen(s => {
      val (gmw, gmh) = maxResolution
      val (smw, smh) = s.maxResolution
      result(gmw min smw, gmh min smh)
    })

  @LuaCallback(value = "get", asynchronous = true)
  def get(context: Context, args: Arguments): Array[Object] = {
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    screen(s => result(s.get(x - 1, y - 1)))
  }

  @LuaCallback("set")
  def set(context: Context, args: Arguments): Array[Object] = {
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val value = args.checkString(2)
    screen(s => {
      s.set(x - 1, y - 1, value)
      result(true)
    })
  }

  @LuaCallback("fill")
  def fill(context: Context, args: Arguments): Array[Object] = {
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val w = args.checkInteger(2)
    val h = args.checkInteger(3)
    val value = args.checkString(4)
    if (value.length == 1)
      screen(s => {
        s.fill(x - 1, y - 1, w, h, value.charAt(0))
        result(true)
      })
    else
      Array(Unit, "invalid fill value")
  }

  @LuaCallback("copy")
  def copy(context: Context, args: Arguments): Array[Object] = {
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val w = args.checkInteger(2)
    val h = args.checkInteger(3)
    val tx = args.checkInteger(4)
    val ty = args.checkInteger(5)
    screen(s => {
      s.copy(x - 1, y - 1, w, h, tx, ty)
      result(true)
    })
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node || screenAddress.exists(_ == node.address)) {
      screenAddress = None
      screenInstance = None
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("oc.gpu.screen")) {
      screenAddress = Some(nbt.getString("oc.gpu.screen"))
      screenInstance = None
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    if (screenAddress.isDefined)
      nbt.setString("oc.gpu.screen", screenAddress.get)
  }
}