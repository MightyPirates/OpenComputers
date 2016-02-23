package li.cil.oc.server.component

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.util.PackedColor
import net.minecraft.nbt.NBTTagCompound

import scala.util.matching.Regex

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

class GraphicsCard(val tier: Int) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("gpu").
    withConnector().
    create()

  private val maxResolution = Settings.screenResolutionsByTier(tier)

  private val maxDepth = Settings.screenDepthsByTier(tier)

  private var screenAddress: Option[String] = None

  private var screenInstance: Option[api.internal.TextBuffer] = None

  private def screen(f: (api.internal.TextBuffer) => Array[AnyRef]) = screenInstance match {
    case Some(screen) => screen.synchronized(f(screen))
    case _ => Array(Unit, "no screen")
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()
    if (node.network != null && screenInstance.isEmpty && screenAddress.isDefined) {
      Option(node.network.node(screenAddress.get)) match {
        case Some(node: Node) if node.host.isInstanceOf[api.internal.TextBuffer] =>
          screenInstance = Some(node.host.asInstanceOf[api.internal.TextBuffer])
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

  @Callback(doc = """function(address:string[, reset:boolean=true]):boolean -- Binds the GPU to the screen with the specified address and resets screen settings if `reset` is true.""")
  def bind(context: Context, args: Arguments): Array[AnyRef] = {
    val address = args.checkString(0)
    val reset = args.optBoolean(1, true)
    node.network.node(address) match {
      case null => result(Unit, "invalid address")
      case node: Node if node.host.isInstanceOf[api.internal.TextBuffer] =>
        screenAddress = Option(address)
        screenInstance = Some(node.host.asInstanceOf[api.internal.TextBuffer])
        screen(s => {
          if (reset) {
            val (gmw, gmh) = maxResolution
            val smw = s.getMaximumWidth
            val smh = s.getMaximumHeight
            s.setResolution(math.min(gmw, smw), math.min(gmh, smh))
            s.setColorDepth(api.internal.TextBuffer.ColorDepth.values.apply(math.min(maxDepth.ordinal, s.getMaximumColorDepth.ordinal)))
            s.setForegroundColor(0xFFFFFF)
            s.setBackgroundColor(0x000000)
          }
          else context.pause(0.2) // To discourage outputting "in realtime" to multiple screens using one GPU.
          result(true)
        })
      case _ => result(Unit, "not a screen")
    }
  }

  @Callback(direct = true, doc = """function():string -- Get the address of the screen the GPU is currently bound to.""")
  def getScreen(context: Context, args: Arguments): Array[AnyRef] = screen(s => result(s.node.address))

  @Callback(direct = true, doc = """function():number, boolean -- Get the current background color and whether it's from the palette or not.""")
  def getBackground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getBackgroundColor, s.isBackgroundFromPalette))

  @Callback(direct = true, doc = """function(value:number[, palette:boolean]):number, number or nil -- Sets the background color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.""")
  def setBackground(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(setBackgroundCosts(tier))
    val color = args.checkInteger(0)
    screen(s => {
      val oldValue = s.getBackgroundColor
      val (oldColor, oldIndex) =
        if (s.isBackgroundFromPalette) {
          (s.getPaletteColor(oldValue), oldValue)
        }
        else {
          (oldValue, Unit)
        }
      s.setBackgroundColor(color, args.optBoolean(1, false))
      result(oldColor, oldIndex)
    })
  }

  final val setBackgroundCosts = Array(1.0 / 32, 1.0 / 64, 1.0 / 128)

  @Callback(direct = true, doc = """function():number, boolean -- Get the current foreground color and whether it's from the palette or not.""")
  def getForeground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getForegroundColor, s.isForegroundFromPalette))

  @Callback(direct = true, doc = """function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.""")
  def setForeground(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(setForegroundCosts(tier))
    val color = args.checkInteger(0)
    screen(s => {
      val oldValue = s.getForegroundColor
      val (oldColor, oldIndex) =
        if (s.isForegroundFromPalette) {
          (s.getPaletteColor(oldValue), oldValue)
        }
        else {
          (oldValue, Unit)
        }
      s.setForegroundColor(color, args.optBoolean(1, false))
      result(oldColor, oldIndex)
    })
  }

  final val setForegroundCosts = Array(1.0 / 32, 1.0 / 64, 1.0 / 128)

  @Callback(direct = true, doc = """function(index:number):number -- Get the palette color at the specified palette index.""")
  def getPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    screen(s => try result(s.getPaletteColor(index)) catch {
      case _: ArrayIndexOutOfBoundsException => throw new IllegalArgumentException("invalid palette index")
    })
  }

  @Callback(direct = true, doc = """function(index:number, color:number):number -- Set the palette color at the specified palette index. Returns the previous value.""")
  def setPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(setPaletteColorCosts(tier))
    val index = args.checkInteger(0)
    val color = args.checkInteger(1)
    context.pause(0.1)
    screen(s => try {
      val oldColor = s.getPaletteColor(index)
      s.setPaletteColor(index, color)
      result(oldColor)
    }
    catch {
      case _: ArrayIndexOutOfBoundsException => throw new IllegalArgumentException("invalid palette index")
    })
  }

  final val setPaletteColorCosts = Array(1.0 / 2, 1.0 / 8, 1.0 / 16)

  @Callback(direct = true, doc = """function():number -- Returns the currently set color depth.""")
  def getDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth.bits(s.getColorDepth)))

  @Callback(doc = """function(depth:number):number -- Set the color depth. Returns the previous value.""")
  def setDepth(context: Context, args: Arguments): Array[AnyRef] = {
    val depth = args.checkInteger(0)
    screen(s => {
      val oldDepth = s.getColorDepth
      depth match {
        case 1 => s.setColorDepth(api.internal.TextBuffer.ColorDepth.OneBit)
        case 4 if maxDepth.ordinal >= api.internal.TextBuffer.ColorDepth.FourBit.ordinal => s.setColorDepth(api.internal.TextBuffer.ColorDepth.FourBit)
        case 8 if maxDepth.ordinal >= api.internal.TextBuffer.ColorDepth.EightBit.ordinal => s.setColorDepth(api.internal.TextBuffer.ColorDepth.EightBit)
        case _ => throw new IllegalArgumentException("unsupported depth")
      }
      result(oldDepth)
    })
  }

  @Callback(direct = true, doc = """function():number -- Get the maximum supported color depth.""")
  def maxDepth(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(PackedColor.Depth.bits(api.internal.TextBuffer.ColorDepth.values.apply(math.min(maxDepth.ordinal, s.getMaximumColorDepth.ordinal)))))

  @Callback(direct = true, doc = """function():number, number -- Get the current screen resolution.""")
  def getResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getWidth, s.getHeight))

  @Callback(doc = """function(width:number, height:number):boolean -- Set the screen resolution. Returns true if the resolution changed.""")
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

  @Callback(direct = true, doc = """function():number, number -- Get the maximum screen resolution.""")
  def maxResolution(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => {
      val (gmw, gmh) = maxResolution
      val smw = s.getMaximumWidth
      val smh = s.getMaximumHeight
      result(math.min(gmw, smw), math.min(gmh, smh))
    })

  @Callback(direct = true, doc = """function():number, number -- Get the current viewport resolution.""")
  def getViewport(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getViewportWidth, s.getViewportHeight))

  @Callback(doc = """function(width:number, height:number):boolean -- Set the viewport resolution. Cannot exceed the screen resolution. Returns true if the resolution changed.""")
  def setViewport(context: Context, args: Arguments): Array[AnyRef] = {
    val w = args.checkInteger(0)
    val h = args.checkInteger(1)
    val (mw, mh) = maxResolution
    // Even though the buffer itself checks this again, we need this here for
    // the minimum of screen and GPU resolution.
    if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
      throw new IllegalArgumentException("unsupported viewport size")
    screen(s => {
      if (w > s.getWidth || h > s.getHeight)
        throw new IllegalArgumentException("unsupported viewport size")
      result(s.setViewport(w, h))
    })
  }

  @Callback(direct = true, doc = """function(x:number, y:number):string, number, number, number or nil, number or nil -- Get the value displayed on the screen at the specified index, as well as the foreground and background color. If the foreground or background is from the palette, returns the palette indices as fourth and fifth results, else nil, respectively.""")
  def get(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    screen(s => {
      val fgValue = s.getForegroundColor(x, y)
      val (fgColor, fgIndex) =
        if (s.isForegroundFromPalette(x, y)) {
          (s.getPaletteColor(fgValue), fgValue)
        }
        else {
          (fgValue, Unit)
        }

      val bgValue = s.getBackgroundColor(x, y)
      val (bgColor, bgIndex) =
        if (s.isBackgroundFromPalette(x, y)) {
          (s.getPaletteColor(bgValue), bgValue)
        }
        else {
          (bgValue, Unit)
        }

      result(s.get(x, y), fgColor, bgColor, fgIndex, bgIndex)
    })
  }

  @Callback(direct = true, doc = """function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Plots a string value to the screen at the specified position. Optionally writes the string vertically.""")
  def set(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(setCosts(tier))
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val value = args.checkString(2)
    val vertical = args.optBoolean(3, false)

    screen(s => {
      if (consumePower(value.length, Settings.get.gpuSetCost)) {
        s.set(x, y, value, vertical)
        result(true)
      }
      else result(Unit, "not enough energy")
    })
  }

  final val setCosts = Array(1.0 / 64, 1.0 / 128, 1.0 / 256)

  @Callback(direct = true, doc = """function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a portion of the screen from the specified location with the specified size by the specified translation.""")
  def copy(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(copyCosts(tier))
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = math.max(0, args.checkInteger(2))
    val h = math.max(0, args.checkInteger(3))
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

  final val copyCosts = Array(1.0 / 16, 1.0 / 32, 1.0 / 64)

  @Callback(direct = true, doc = """function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a portion of the screen at the specified position with the specified size with the specified character.""")
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    context.consumeCallBudget(fillCosts(tier))
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = math.max(0, args.checkInteger(2))
    val h = math.max(0, args.checkInteger(3))
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

  final val fillCosts = Array(1.0 / 32, 1.0 / 64, 1.0 / 128)

  private def consumePower(n: Double, cost: Double) = node.tryChangeBuffer(-n * cost)

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.name == "computer.stopped" && node.isNeighborOf(message.source)) {
      screen(s => {
        val (gmw, gmh) = maxResolution
        val smw = s.getMaximumWidth
        val smh = s.getMaximumHeight
        s.setResolution(math.min(gmw, smw), math.min(gmh, smh))
        s.setColorDepth(api.internal.TextBuffer.ColorDepth.values.apply(math.min(maxDepth.ordinal, s.getMaximumColorDepth.ordinal)))
        s.setForegroundColor(0xFFFFFF)
        val w = s.getWidth
        val h = s.getHeight
        message.source.host match {
          case machine: li.cil.oc.server.machine.Machine if machine.lastError != null =>
            if (s.getColorDepth.ordinal > api.internal.TextBuffer.ColorDepth.OneBit.ordinal) s.setBackgroundColor(0x0000FF)
            else s.setBackgroundColor(0x000000)
            s.fill(0, 0, w, h, ' ')
            try {
              val wrapRegEx = s"(.{1,${math.max(1, w - 2)}})\\s".r
              val lines = wrapRegEx.replaceAllIn(Localization.localizeImmediately(machine.lastError).replace("\t", "  ") + "\n", m => Regex.quoteReplacement(m.group(1) + "\n")).lines.toArray
              val firstRow = ((h - lines.length) / 2) max 2

              val message = "Unrecoverable Error"
              s.set((w - message.length) / 2, firstRow - 2, message, false)

              val maxLineLength = lines.map(_.length).max
              val col = ((w - maxLineLength) / 2) max 0
              for ((line, idx) <- lines.zipWithIndex) {
                val row = firstRow + idx
                s.set(col, row, line, false)
              }
            }
            catch {
              case t: Throwable => t.printStackTrace()
            }
          case _ =>
            s.setBackgroundColor(0x000000)
            s.fill(0, 0, w, h, ' ')
        }
        null // For screen()
      })
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node || screenAddress.contains(node.address)) {
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
