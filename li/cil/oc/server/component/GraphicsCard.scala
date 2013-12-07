package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.common.component.Buffer
import li.cil.oc.util.PackedColor
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import scala.Some

abstract class GraphicsCard extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("gpu").
    withConnector().
    create()

  val maxResolution: (Int, Int)

  val maxDepth: PackedColor.Depth.Value

  private var screenAddress: Option[String] = None

  private var screenInstance: Option[Buffer] = None

  private def screen(f: (Buffer) => Array[AnyRef]) = screenInstance match {
    case Some(screen) => screen.synchronized(f(screen))
    case _ => Array(Unit, "no screen")
  }

  // ----------------------------------------------------------------------- //

  override def update() {
    super.update()
    if (screenInstance.isEmpty && screenAddress.isDefined) {
      Option(node.network.node(screenAddress.get)) match {
        case Some(node: Node) if node.host.isInstanceOf[Buffer] =>
          screenInstance = Some(node.host.asInstanceOf[Buffer])
        case _ =>
          // This could theoretically happen after loading an old address, but
          // if the screen either disappeared between saving and now or changed
          // type. The first scenario is more likely, and could happen if the
          // chunk the screen is in isn't loaded when the chunk the GPU is in
          // gets loaded.
          screenAddress = None
      }
    }
  }

  @LuaCallback("bind")
  def bind(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    node.network.node(address) match {
      case null => result(false, "invalid address")
      case node: Node if node.host.isInstanceOf[Buffer] =>
        screenAddress = Option(address)
        screenInstance = Some(node.host.asInstanceOf[Buffer])
        screen(s => {
          val (gmw, gmh) = maxResolution
          val (smw, smh) = s.maxResolution
          s.resolution = (gmw min smw, gmh min smh)
          s.depth = PackedColor.Depth(maxDepth.id min s.maxDepth.id)
          s.foreground = 0xFFFFFF
          s.background = 0x000000
          result(true)
        })
      case _ => result(false, "not a screen")
    }
  }

  @LuaCallback(value = "getBackground", direct = true)
  def getBackground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.background))

  def setBackground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => result(s.background = color))
  }

  @LuaCallback(value = "getForeground", direct = true)
  def getForeground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.foreground))

  def setForeground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => result(s.foreground = color))
  }

  @LuaCallback(value = "getDepth", direct = true)
  def getDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth.bits(s.depth)))

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
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    screen(s => result(s.get(x, y)))
  }

  def set(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val value = args.checkString(2)

    screen(s => {
      if (consumePower(value.length, Settings.get.gpuSetCost)) {
        s.set(x, y, value)
        result(true)
      }
      else result(false)
    })
  }

  def copy(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = args.checkInteger(2)
    val h = args.checkInteger(3)
    val tx = args.checkInteger(4)
    val ty = args.checkInteger(5)
    screen(s => {
      if (consumePower(w * h, Settings.get.gpuCopyCost)) {
        s.copy(x, y, w, h, tx, ty)
        result(true)
      }
      else result(false)
    })
  }

  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = args.checkInteger(2)
    val h = args.checkInteger(3)
    val value = args.checkString(4)
    if (value.length == 1) screen(s => {
      val c = value.charAt(0)
      val cost = if (c == ' ') Settings.get.gpuClearCost else Settings.get.gpuFillCost
      if (consumePower(w * h, cost)) {
        s.fill(x, y, w, h, value.charAt(0))
        result(true)
      }
      else {
        result(false)
      }
    })
    else throw new Exception("invalid fill value")
  }

  private def consumePower(n: Double, cost: Double) = node.tryChangeBuffer(-n * cost)

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

    if (nbt.hasKey("screen")) {
      screenAddress = Some(nbt.getString("screen"))
      screenInstance = None
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    if (screenAddress.isDefined) {
      nbt.setString("screen", screenAddress.get)
    }
  }
}

object GraphicsCard {

  // IMPORTANT: usually methods with side effects should *not* be direct
  // callbacks to avoid the massive headache synchronizing them ensues, in
  // particular when it comes to world saving. I'm making an exception for
  // screens, though since they'd be painfully sluggish otherwise. This also
  // means we have to use a somewhat nasty trick in common.component.Buffer's
  // save function: we wait for all computers in the same network to finish
  // their current execution and then pause them, to ensure the state of the
  // buffer is "clean", meaning the computer has the correct state when it is
  // saved in turn. If we didn't, a computer might change a screen after it was
  // saved, but before the computer was saved, leading to mismatching states in
  // the save file - a Bad Thing (TM).

  class Tier1 extends GraphicsCard {
    val maxDepth = Settings.screenDepthsByTier(0)
    val maxResolution = Settings.screenResolutionsByTier(0)

    @LuaCallback(value = "copy", direct = true, limit = 1)
    override def copy(context: Context, args: Arguments) = super.copy(context, args)

    @LuaCallback(value = "fill", direct = true, limit = 1)
    override def fill(context: Context, args: Arguments) = super.fill(context, args)

    @LuaCallback(value = "set", direct = true, limit = 4)
    override def set(context: Context, args: Arguments) = super.set(context, args)

    @LuaCallback(value = "setBackground", direct = true, limit = 2)
    override def setBackground(context: Context, args: Arguments) = super.setBackground(context, args)

    @LuaCallback(value = "setForeground", direct = true, limit = 2)
    override def setForeground(context: Context, args: Arguments) = super.setForeground(context, args)
  }

  class Tier2 extends GraphicsCard {
    val maxDepth = Settings.screenDepthsByTier(1)
    val maxResolution = Settings.screenResolutionsByTier(1)

    @LuaCallback(value = "copy", direct = true, limit = 2)
    override def copy(context: Context, args: Arguments) = super.copy(context, args)

    @LuaCallback(value = "fill", direct = true, limit = 4)
    override def fill(context: Context, args: Arguments) = super.fill(context, args)

    @LuaCallback(value = "set", direct = true, limit = 8)
    override def set(context: Context, args: Arguments) = super.set(context, args)

    @LuaCallback(value = "setBackground", direct = true, limit = 4)
    override def setBackground(context: Context, args: Arguments) = super.setBackground(context, args)

    @LuaCallback(value = "setForeground", direct = true, limit = 4)
    override def setForeground(context: Context, args: Arguments) = super.setForeground(context, args)
  }

  class Tier3 extends GraphicsCard {
    val maxDepth = Settings.screenDepthsByTier(2)
    val maxResolution = Settings.screenResolutionsByTier(2)

    @LuaCallback(value = "copy", direct = true, limit = 4)
    override def copy(context: Context, args: Arguments) = super.copy(context, args)

    @LuaCallback(value = "fill", direct = true, limit = 8)
    override def fill(context: Context, args: Arguments) = super.fill(context, args)

    @LuaCallback(value = "set", direct = true, limit = 16)
    override def set(context: Context, args: Arguments) = super.set(context, args)

    @LuaCallback(value = "setBackground", direct = true, limit = 8)
    override def setBackground(context: Context, args: Arguments) = super.setBackground(context, args)

    @LuaCallback(value = "setForeground", direct = true, limit = 8)
    override def setForeground(context: Context, args: Arguments) = super.setForeground(context, args)
  }

}