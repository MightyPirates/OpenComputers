package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.component.Screen.ColorDepth
import li.cil.oc.api.network._
import li.cil.oc.common.component.ManagedComponent
import li.cil.oc.common.tileentity
import li.cil.oc.util.PackedColor
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.StatCollector
import li.cil.oc.api.component.Screen

abstract class GraphicsCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("gpu").
    withConnector().
    create()

  protected val maxResolution: (Int, Int)

  protected val maxDepth: ColorDepth

  private var screenAddress: Option[String] = None

  private var screenInstance: Option[Screen] = None

  private def screen(f: (Screen) => Array[AnyRef]) = screenInstance match {
    case Some(screen) => screen.synchronized(f(screen))
    case _ => Array(Unit, "no screen")
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()
    if (node.network != null && screenInstance.isEmpty && screenAddress.isDefined) {
      Option(node.network.node(screenAddress.get)) match {
        case Some(node: Node) if node.host.isInstanceOf[Screen] =>
          screenInstance = Some(node.host.asInstanceOf[Screen])
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

  @Callback
  def bind(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    node.network.node(address) match {
      case null => result(Unit, "invalid address")
      case node: Node if node.host.isInstanceOf[Screen] =>
        screenAddress = Option(address)
        screenInstance = Some(node.host.asInstanceOf[Screen])
        screen(s => {
          val (gmw, gmh) = maxResolution
          val smw = s.getMaximumWidth
          val smh = s.getMaximumHeight
          s.setResolution(math.min(gmw, smw), math.min(gmh, smh))
          s.setColorDepth(ColorDepth.values.apply(math.min(maxDepth.ordinal, s.getMaximumColorDepth.ordinal)))
          s.setForegroundColor(0xFFFFFF)
          s.setBackgroundColor(0x000000)
          result(true)
        })
      case _ => result(Unit, "not a screen")
    }
  }

  @Callback(direct = true)
  def getBackground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getBackgroundColor, s.isBackgroundFromPalette))

  def setBackground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => {
      val oldColor = s.getBackgroundColor
      val oldIsPalette = s.isBackgroundFromPalette
      s.setBackgroundColor(color, args.count > 1 && args.checkBoolean(1))
      result(oldColor, oldIsPalette)
    })
  }

  @Callback(direct = true)
  def getForeground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getForegroundColor, s.isForegroundFromPalette))

  def setForeground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => {
      val oldColor = s.getForegroundColor
      val oldIsPalette = s.isForegroundFromPalette
      s.setForegroundColor(color, args.count > 1 && args.checkBoolean(1))
      result(oldColor, oldIsPalette)
    })
  }

  @Callback(direct = true)
  def getPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    screen(s => result(s.getPaletteColor(index)))
  }

  @Callback
  def setPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    val color = args.checkInteger(1)
    context.pause(0.1)
    screen(s => {
      val oldColor = s.getPaletteColor(index)
      s.setPaletteColor(index, color)
      result(oldColor)
    })
  }

  @Callback(direct = true)
  def getDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth.bits(s.getColorDepth)))

  @Callback
  def setDepth(context: Context, args: Arguments): Array[AnyRef] = {
    val depth = args.checkInteger(0)
    screen(s => {
      val oldDepth = s.getColorDepth
      depth match {
        case 1 => s.setColorDepth(ColorDepth.OneBit)
        case 4 if maxDepth.ordinal >= ColorDepth.FourBit.ordinal => s.setColorDepth(ColorDepth.FourBit)
        case 8 if maxDepth.ordinal >= ColorDepth.EightBit.ordinal => s.setColorDepth(ColorDepth.EightBit)
        case _ => throw new IllegalArgumentException("unsupported depth")
      }
      result(oldDepth)
    })
  }

  @Callback(direct = true)
  def maxDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth.bits(ColorDepth.values.apply(math.min(maxDepth.ordinal, s.getMaximumColorDepth.ordinal)))))

  @Callback(direct = true)
  def getResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getWidth, s.getHeight))

  @Callback
  def setResolution(context: Context, args: Arguments): Array[AnyRef] = {
    val w = args.checkInteger(0)
    val h = args.checkInteger(1)
    val (mw, mh) = maxResolution
    // Even though the buffer itself checks this again, we need this here for
    // the minimum of screen and GPU resolution.
    if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
      throw new IllegalArgumentException("unsupported resolution")
    screen(s => result(s.setResolution(w, h)))
  }

  @Callback(direct = true)
  def maxResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => {
      val (gmw, gmh) = maxResolution
      val smw = s.getMaximumWidth
      val smh = s.getMaximumHeight
      result(math.min(gmw, smw), math.min(gmh, smh))
    })

  @Callback(direct = true)
  def get(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    screen(s => {
      result(s.get(x, y), s.getForegroundColor, s.getBackgroundColor, s.isForegroundFromPalette, s.isBackgroundFromPalette)
    })
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
      else result(Unit, "not enough energy")
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
      else result(Unit, "not enough energy")
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
        result(Unit, "not enough energy")
      }
    })
    else throw new Exception("invalid fill value")
  }

  private def consumePower(n: Double, cost: Double) = node.tryChangeBuffer(-n * cost)

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.name == "computer.stopped" && node.isNeighborOf(message.source)) {
      screenInstance match {
        case Some(buffer) => buffer.synchronized {
          val w = buffer.getWidth
          val h = buffer.getHeight
          buffer.setForegroundColor(0xFFFFFF)
          message.source.host match {
            case machine: machine.Machine if machine.lastError != null =>
              if (buffer.getColorDepth.ordinal > ColorDepth.OneBit.ordinal) buffer.setBackgroundColor(0x0000FF)
              else buffer.setBackgroundColor(0x000000)
              buffer.fill(0, 0, w, h, ' ')
              try {
                val message = "Unrecoverable error:\n" + StatCollector.translateToLocal(machine.lastError) + "\n"
                val wrapRegEx = s"(.{1,${math.max(1, w - 2)}})\\s".r
                val lines = wrapRegEx.replaceAllIn(message, m => m.group(1) + "\n").lines.toArray
                for ((line, idx) <- lines.zipWithIndex) {
                  val col = (w - line.length) / 2
                  val row = (h - lines.length) / 2 + idx
                  buffer.set(col, row, line)
                }
              }
              catch {
                case t: Throwable => t.printStackTrace()
              }
            case _ =>
              buffer.setBackgroundColor(0x000000)
              buffer.fill(0, 0, w, h, ' ')
          }
        }
        case _ =>
      }
    }
  }

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
      nbt.getString("screen") match {
        case screen: String if !screen.isEmpty => screenAddress = Some(screen)
        case _ => screenAddress = None
      }
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
    protected val maxDepth = Settings.screenDepthsByTier(0)
    protected val maxResolution = Settings.screenResolutionsByTier(0)

    @Callback(direct = true, limit = 1)
    override def copy(context: Context, args: Arguments) = super.copy(context, args)

    @Callback(direct = true, limit = 1)
    override def fill(context: Context, args: Arguments) = super.fill(context, args)

    @Callback(direct = true, limit = 4)
    override def set(context: Context, args: Arguments) = super.set(context, args)

    @Callback(direct = true, limit = 2)
    override def setBackground(context: Context, args: Arguments) = super.setBackground(context, args)

    @Callback(direct = true, limit = 2)
    override def setForeground(context: Context, args: Arguments) = super.setForeground(context, args)
  }

  class Tier2 extends GraphicsCard {
    protected val maxDepth = Settings.screenDepthsByTier(1)
    protected val maxResolution = Settings.screenResolutionsByTier(1)

    @Callback(direct = true, limit = 2)
    override def copy(context: Context, args: Arguments) = super.copy(context, args)

    @Callback(direct = true, limit = 4)
    override def fill(context: Context, args: Arguments) = super.fill(context, args)

    @Callback(direct = true, limit = 8)
    override def set(context: Context, args: Arguments) = super.set(context, args)

    @Callback(direct = true, limit = 4)
    override def setBackground(context: Context, args: Arguments) = super.setBackground(context, args)

    @Callback(direct = true, limit = 4)
    override def setForeground(context: Context, args: Arguments) = super.setForeground(context, args)
  }

  class Tier3 extends GraphicsCard {
    protected val maxDepth = Settings.screenDepthsByTier(2)
    protected val maxResolution = Settings.screenResolutionsByTier(2)

    @Callback(direct = true, limit = 4)
    override def copy(context: Context, args: Arguments) = super.copy(context, args)

    @Callback(direct = true, limit = 8)
    override def fill(context: Context, args: Arguments) = super.fill(context, args)

    @Callback(direct = true, limit = 16)
    override def set(context: Context, args: Arguments) = super.set(context, args)

    @Callback(direct = true, limit = 8)
    override def setBackground(context: Context, args: Arguments) = super.setBackground(context, args)

    @Callback(direct = true, limit = 8)
    override def setForeground(context: Context, args: Arguments) = super.setForeground(context, args)
  }

}