package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.common.component.Buffer
import li.cil.oc.util.PackedColor
import li.cil.oc.{Config, api}
import net.minecraft.nbt.NBTTagCompound
import scala.Some

class GraphicsCard(val maxResolution: (Int, Int), val maxDepth: PackedColor.Depth.Value) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("gpu").
    create()

  private var screenAddress: Option[String] = None

  private var screenInstance: Option[Buffer] = None

  private def screen(f: (Buffer) => Array[AnyRef]) = this.synchronized {
    if (screenInstance.isEmpty && screenAddress.isDefined) {
      Option(node.network.node(screenAddress.get)) match {
        case Some(node: Node) if node.host.isInstanceOf[Buffer.Environment] =>
          screenInstance = Some(node.host.asInstanceOf[Buffer.Environment].instance)
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
  def bind(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val address = args.checkString(0)
    node.network.node(address) match {
      case null => Array(Unit, "invalid address")
      case node: Node if node.host.isInstanceOf[Buffer.Environment] =>
        screenAddress = Option(address)
        screenInstance = None
        screen(s => {
          val (gmw, gmh) = maxResolution
          val (smw, smh) = s.maxResolution
          s.resolution = (gmw min smw, gmh min smh)
          s.depth = PackedColor.Depth(maxDepth.id min s.maxDepth.id)
          s.foreground = 0xFFFFFF
          s.background = 0x000000
          result(true)
        })
      case _ => Array(Unit, "not a screen")
    }
  }

  @LuaCallback(value = "getBackground", direct = true)
  def getBackground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.background))

  @LuaCallback("setBackground")
  def setBackground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => result(s.background = color))
  }

  @LuaCallback(value = "getForeground", direct = true)
  def getForeground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.foreground))

  @LuaCallback("setForeground")
  def setForeground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => result(s.foreground = color))
  }

  @LuaCallback(value = "getDepth", direct = true)
  def getDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.depth match {
      case PackedColor.Depth.OneBit => 1
      case PackedColor.Depth.FourBit => 4
      case PackedColor.Depth.EightBit => 8
    }))

  @LuaCallback("setDepth")
  def setDepth(context: Context, args: Arguments): Array[AnyRef] = {
    val depth = args.checkInteger(0)
    screen(s => result(s.depth = depth match {
      case 1 => PackedColor.Depth.OneBit
      case 4 if maxDepth >= PackedColor.Depth.FourBit => PackedColor.Depth.FourBit
      case 8 if maxDepth >= PackedColor.Depth.EightBit => PackedColor.Depth.EightBit
      case _ => throw new IllegalArgumentException("unsupported depth")
    }))
  }

  @LuaCallback(value = "maxDepth", direct = true)
  def maxDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth(maxDepth.id min s.maxDepth.id) match {
      case PackedColor.Depth.OneBit => 1
      case PackedColor.Depth.FourBit => 4
      case PackedColor.Depth.EightBit => 8
    }))

  @LuaCallback(value = "getResolution", direct = true)
  def getResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => {
      val (w, h) = s.resolution
      result(w, h)
    })

  @LuaCallback("setResolution")
  def setResolution(context: Context, args: Arguments): Array[AnyRef] = {
    val w = args.checkInteger(0)
    val h = args.checkInteger(1)
    val (mw, mh) = maxResolution
    if (w > 0 && h > 0 && w <= mw && h <= mh) screen(s => result(s.resolution = (w, h)))
    else throw new IllegalArgumentException("unsupported resolution")
  }

  @LuaCallback(value = "maxResolution", direct = true)
  def maxResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => {
      val (gmw, gmh) = maxResolution
      val (smw, smh) = s.maxResolution
      result(gmw min smw, gmh min smh)
    })

  @LuaCallback(value = "get", direct = true)
  def get(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    screen(s => result(s.get(x - 1, y - 1)))
  }

  @LuaCallback("set")
  def set(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val value = args.checkString(2)
    screen(s => {
      s.set(x - 1, y - 1, value)
      result(true)
    })
  }

  @LuaCallback("fill")
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
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
  def copy(context: Context, args: Arguments): Array[AnyRef] = {
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
    if (nbt.hasKey(Config.namespace + "gpu.screen")) {
      screenAddress = Some(nbt.getString(Config.namespace + "gpu.screen"))
      screenInstance = None
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    if (screenAddress.isDefined) {
      nbt.setString(Config.namespace + "gpu.screen", screenAddress.get)
    }
  }
}