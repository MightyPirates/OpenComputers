package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component.Buffer
import li.cil.oc.common.tileentity
import li.cil.oc.Settings
import li.cil.oc.util.PackedColor
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.StatCollector

abstract class GraphicsCard extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Neighbors).
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

  override val canUpdate = true

  override def update() {
    super.update()
    if (node.network != null && screenInstance.isEmpty && screenAddress.isDefined) {
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

  @Callback
  def bind(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    node.network.node(address) match {
      case null => result(Unit, "invalid address")
      case node: Node if node.host.isInstanceOf[Buffer] =>
        screenAddress = Option(address)
        screenInstance = Some(node.host.asInstanceOf[Buffer])
        screen(s => {
          val (gmw, gmh) = maxResolution
          val (smw, smh) = s.maxResolution
          s.resolution = (math.min(gmw, smw), math.min(gmh, smh))
          s.format = PackedColor.Depth.format(PackedColor.Depth(math.min(maxDepth.id, s.maxDepth.id)))
          s.foreground = PackedColor.Color(0xFFFFFF)
          s.background = PackedColor.Color(0x000000)
          result(true)
        })
      case _ => result(Unit, "not a screen")
    }
  }

  @Callback(direct = true)
  def getBackground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.background.value, s.background.isPalette))

  def setBackground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => {
      val background = s.background = PackedColor.Color(color, args.count > 1 && args.checkBoolean(1))
      result(background.value, background.isPalette)
    })
  }

  @Callback(direct = true)
  def getForeground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.foreground.value, s.foreground.isPalette))

  def setForeground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    screen(s => {
      val foreground = s.foreground = PackedColor.Color(color, args.count > 1 && args.checkBoolean(1))
      result(foreground.value, foreground.isPalette)
    })
  }

  @Callback(direct = true)
  def getPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    screen(s => result(s.getPalette(index)))
  }

  @Callback
  def setPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    val color = args.checkInteger(1)
    context.pause(0.1)
    screen(s => result(s.setPalette(index, color)))
  }

  @Callback(direct = true)
  def getDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth.bits(s.format.depth)))

  @Callback
  def setDepth(context: Context, args: Arguments): Array[AnyRef] = {
    val depth = args.checkInteger(0)
    screen(s => result(s.format = depth match {
      case 1 => PackedColor.Depth.format(PackedColor.Depth.OneBit)
      case 4 if maxDepth >= PackedColor.Depth.FourBit => PackedColor.Depth.format(PackedColor.Depth.FourBit)
      case 8 if maxDepth >= PackedColor.Depth.EightBit => PackedColor.Depth.format(PackedColor.Depth.EightBit)
      case _ => throw new IllegalArgumentException("unsupported depth")
    }))
  }

  @Callback(direct = true)
  def maxDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth(math.min(maxDepth.id, s.maxDepth.id)) match {
      case PackedColor.Depth.OneBit => 1
      case PackedColor.Depth.FourBit => 4
      case PackedColor.Depth.EightBit => 8
    }))

  @Callback(direct = true)
  def getResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => {
      val (w, h) = s.resolution
      result(w, h)
    })

  @Callback
  def setResolution(context: Context, args: Arguments): Array[AnyRef] = {
    val w = args.checkInteger(0)
    val h = args.checkInteger(1)
    val (mw, mh) = maxResolution
    // Even though the buffer itself checks this again, we need this here for
    // the minimum of screen and GPU resolution.
    if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
      throw new IllegalArgumentException("unsupported resolution")
    screen(s => result(s.resolution = (w, h)))
  }

  @Callback(direct = true)
  def maxResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => {
      val (gmw, gmh) = maxResolution
      val (smw, smh) = s.maxResolution
      result(math.min(gmw, smw), math.min(gmh, smh))
    })

  @Callback
  def getSize(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => s.owner match {
      case screen: tileentity.Screen => result(screen.width, screen.height)
      case _ => result(1, 1)
    })

  @Callback(direct = true)
  def get(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    screen(s => {
      val char = s.get(x, y)
      val color = s.color(y)(x)
      val format = s.format
      val foreground = PackedColor.unpackForeground(color, format)
      val background = PackedColor.unpackBackground(color, format)
      result(char, foreground, background)
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
          val (w, h) = buffer.resolution
          buffer.foreground = PackedColor.Color(0xFFFFFF)
          message.source.host match {
            case machine: machine.Machine if machine.lastError != null =>
              if (buffer.format.depth > PackedColor.Depth.OneBit) buffer.background = PackedColor.Color(0x0000FF)
              else buffer.background = PackedColor.Color(0x000000)
              if (buffer.buffer.fill(0, 0, w, h, ' ')) {
                buffer.owner.onScreenFill(0, 0, w, h, ' ')
              }
              try {
                val message = "Unrecoverable error:\n" + StatCollector.translateToLocal(machine.lastError) + "\n"
                val wrapRegEx = s"(.{1,${math.max(1, w - 2)}})\\s".r
                val lines = wrapRegEx.replaceAllIn(message, m => m.group(1) + "\n").lines.toArray
                for ((line, idx) <- lines.zipWithIndex) {
                  val col = (w - line.length) / 2
                  val row = (h - lines.length) / 2 + idx
                  buffer.buffer.set(col, row, line)
                  buffer.owner.onScreenSet(col, row, line)
                }
              }
              catch {
                case t: Throwable => t.printStackTrace()
              }
            case _ =>
              buffer.background = PackedColor.Color(0x000000)
              if (buffer.buffer.fill(0, 0, w, h, ' ')) {
                buffer.owner.onScreenFill(0, 0, w, h, ' ')
              }
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
    val maxDepth = Settings.screenDepthsByTier(0)
    val maxResolution = Settings.screenResolutionsByTier(0)

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
    val maxDepth = Settings.screenDepthsByTier(1)
    val maxResolution = Settings.screenResolutionsByTier(1)

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
    val maxDepth = Settings.screenDepthsByTier(2)
    val maxResolution = Settings.screenResolutionsByTier(2)

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