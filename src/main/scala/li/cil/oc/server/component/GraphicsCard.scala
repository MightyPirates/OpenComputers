package li.cil.oc.server.component

import java.util

import li.cil.oc.{Constants, Localization, Settings, api}
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.machine.{Arguments, Callback, Context, LimitReachedException}
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.PackedColor
import net.minecraft.nbt.{CompoundNBT, ListNBT}
import li.cil.oc.common.component
import li.cil.oc.common.component.GpuTextBuffer

import scala.collection.convert.WrapAsJava._
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

class GraphicsCard(val tier: Int) extends AbstractManagedEnvironment with DeviceInfo with component.traits.VideoRamDevice {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("gpu").
    withConnector().
    create()

  private val maxResolution = Settings.screenResolutionsByTier(tier)

  private val maxDepth = Settings.screenDepthsByTier(tier)

  private var screenAddress: Option[String] = None

  private var screenInstance: Option[api.internal.TextBuffer] = None

  private var bufferIndex: Int = RESERVED_SCREEN_INDEX // screen is index zero

  private def screen(index: Int, f: (api.internal.TextBuffer) => Array[AnyRef]): Array[AnyRef] = {
    if (index == RESERVED_SCREEN_INDEX) {
      screenInstance match {
        case Some(screen) => screen.synchronized(f(screen))
        case _ => Array(Unit, "no screen")
      }
    } else {
      getBuffer(index) match {
        case Some(buffer: api.internal.TextBuffer) => f(buffer)
        case _ => Array(Unit, "invalid buffer index")
      }
    }
  }

  private def screen(f: (api.internal.TextBuffer) => Array[AnyRef]): Array[AnyRef] = screen(bufferIndex, f)

  final val setBackgroundCosts = Array(1.0 / 32, 1.0 / 64, 1.0 / 128)
  final val setForegroundCosts = Array(1.0 / 32, 1.0 / 64, 1.0 / 128)
  final val setPaletteColorCosts = Array(1.0 / 2, 1.0 / 8, 1.0 / 16)
  final val setCosts = Array(1.0 / 64, 1.0 / 128, 1.0 / 256)
  final val copyCosts = Array(1.0 / 16, 1.0 / 32, 1.0 / 64)
  final val fillCosts = Array(1.0 / 32, 1.0 / 64, 1.0 / 128)
  // These are dirty page bitblt budget costs
  // a single bitblt can send a screen of data, which is n*set calls where set is writing an entire line
  // So for each tier, we multiple the set cost with the number of lines the screen may have
  final val bitbltCost: Double = Settings.get.bitbltCost * scala.math.pow(2, tier)
  final val totalVRAM: Double = (maxResolution._1 * maxResolution._2) * Settings.get.vramSizes(0 max tier min 2)

  var budgetExhausted: Boolean = false // for especially expensive calls, bitblt

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Display,
    DeviceAttribute.Description -> "Graphics controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> ("MPG" + ((tier + 1) * 1000).toString + " GTZ"),
    DeviceAttribute.Capacity -> capacityInfo,
    DeviceAttribute.Width -> widthInfo,
    DeviceAttribute.Clock -> clockInfo
  )

  def capacityInfo: String = (maxResolution._1 * maxResolution._2).toString

  def widthInfo: String = Array("1", "4", "8").apply(maxDepth.ordinal())

  def clockInfo: String = ((2000 / setBackgroundCosts(tier)).toInt / 100).toString + "/" + ((2000 / setForegroundCosts(tier)).toInt / 100).toString + "/" + ((2000 / setPaletteColorCosts(tier)).toInt / 100).toString + "/" + ((2000 / setCosts(tier)).toInt / 100).toString + "/" + ((2000 / copyCosts(tier)).toInt / 100).toString + "/" + ((2000 / fillCosts(tier)).toInt / 100).toString

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  private def resolveInvokeCosts(idx: Int, context: Context, budgetCost: Double, units: Int, factor: Double): Boolean = {
    idx match {
      case RESERVED_SCREEN_INDEX =>
        context.consumeCallBudget(budgetCost)
        consumePower(units, factor)
      case _ => true
    }
  }

  @Callback(direct = true, doc = """function(): number -- returns the index of the currently selected buffer. 0 is reserved for the screen. Can return 0 even when there is no screen""")
  def getActiveBuffer(context: Context, args: Arguments): Array[AnyRef] = {
    result(bufferIndex)
  }

  @Callback(direct = true, doc = """function(index: number): number -- Sets the active buffer to `index`. 1 is the first vram buffer and 0 is reserved for the screen. returns nil for invalid index (0 is always valid)""")
  def setActiveBuffer(context: Context, args: Arguments): Array[AnyRef] = {
    val previousIndex: Int = bufferIndex
    val newIndex: Int = args.checkInteger(0)
    if (newIndex != RESERVED_SCREEN_INDEX && getBuffer(newIndex).isEmpty) {
      result(Unit, "invalid buffer index")
    } else {
      bufferIndex = newIndex
      if (bufferIndex == RESERVED_SCREEN_INDEX) {
        screen(s => result(true))
      }
      result(previousIndex)
    }
  }

  @Callback(direct = true, doc = """function(): number -- Returns an array of indexes of the allocated buffers""")
  def buffers(context: Context, args: Arguments): Array[AnyRef] = {
    result(bufferIndexes())
  }
  
  @Callback(direct = true, doc = """function([width: number, height: number]): number -- allocates a new buffer with dimensions width*height (defaults to max resolution) and appends it to the buffer list. Returns the index of the new buffer and returns nil with an error message on failure. A buffer can be allocated even when there is no screen bound to this gpu. Index 0 is always reserved for the screen and thus the lowest index of an allocated buffer is always 1.""")
  def allocateBuffer(context: Context, args: Arguments): Array[AnyRef] = {
    val width: Int = args.optInteger(0, maxResolution._1)
    val height: Int = args.optInteger(1, maxResolution._2)
    val size: Int = width * height
    if (width <= 0 || height <= 0) {
      result(Unit, "invalid page dimensions: must be greater than zero")
    }
    else if (size > (totalVRAM - calculateUsedMemory)) {
      result(Unit, "not enough video memory")
    } else if (node == null) {
      result(Unit, "graphics card appears disconnected")
    } else {
      val format: PackedColor.ColorFormat = PackedColor.Depth.format(Settings.screenDepthsByTier(tier))
      val buffer = new li.cil.oc.util.TextBuffer(width, height, format)
      val page = component.GpuTextBuffer.wrap(node.address, nextAvailableBufferIndex, buffer)
      addBuffer(page)
      result(page.id)
    }
  }

  // this event occurs when the gpu is told a page was removed - we need to notify the screen of this
  // we do this because the VideoRamDevice trait only notifies itself, it doesn't assume there is a screen
  override def onBufferRamDestroy(id: Int): Unit = {
    // first protect our buffer index - it needs to fall back to the screen if its buffer was removed
    if (id != RESERVED_SCREEN_INDEX) {
      screen(RESERVED_SCREEN_INDEX, s => s match {
        case oc: component.traits.VideoRamRasterizer => result(oc.removeBuffer(node.address, id))
        case _ => result(true)// addon mod screen type that is not video ram aware
      })
    }
    if (id == bufferIndex) {
      bufferIndex = RESERVED_SCREEN_INDEX
    }
  }

  @Callback(direct = true, doc = """function(index: number): boolean -- Closes buffer at `index`. Returns true if a buffer closed. If the current buffer is closed, index moves to 0""")
  def freeBuffer(context: Context, args: Arguments): Array[AnyRef] = {
    val index: Int = args.optInteger(0, bufferIndex)
    if (removeBuffers(Array(index)) == 1) result(true)
    else result(Unit, "no buffer at index")
  }

  @Callback(direct = true, doc = """function(): number -- Closes all buffers and returns the count. If the active buffer is closed, index moves to 0""")
  def freeAllBuffers(context: Context, args: Arguments): Array[AnyRef] = result(removeAllBuffers())

  @Callback(direct = true, doc = """function(): number -- returns the total memory size of the gpu vram. This does not include the screen.""")
  def totalMemory(context: Context, args: Arguments): Array[AnyRef] = {
    result(totalVRAM)
  }

  @Callback(direct = true, doc = """function(): number -- returns the total free memory not allocated to buffers. This does not include the screen.""")
  def freeMemory(context: Context, args: Arguments): Array[AnyRef] = {
    result(totalVRAM - calculateUsedMemory)
  }

  @Callback(direct = true, doc = """function(index: number): number, number -- returns the buffer size at index. Returns the screen resolution for index 0. returns nil for invalid indexes""")
  def getBufferSize(context: Context, args: Arguments): Array[AnyRef] = {
    val idx = args.optInteger(0, bufferIndex)
    screen(idx, s => result(s.getWidth, s.getHeight))
  }

  private def determineBitbltBudgetCost(dst: api.internal.TextBuffer, src: api.internal.TextBuffer): Double = {
    // large dirty buffers need throttling so their budget cost is more
    // clean buffers have no budget cost.
    src match {
      case page: GpuTextBuffer => dst match {
        case _: GpuTextBuffer => 0.0 // no cost to write to ram
        case _ if page.dirty => // screen target will need the new buffer
          // small buffers are cheap, so increase with size of buffer source
          bitbltCost * (src.getWidth * src.getHeight) / (maxResolution._1 * maxResolution._2)
        case _ => .001 // bitblt a clean page to screen has a minimal cost
      }
      case _ => 0.0 // from screen is free
    }
  }

  private def determineBitbltEnergyCost(dst: api.internal.TextBuffer): Double = {
    // memory to memory copies are extremely energy efficient
    // rasterizing to the screen has the same cost as copy (in fact, screen-to-screen blt _is_ a copy
    dst match {
      case _: GpuTextBuffer => 0
      case _ => Settings.get.gpuCopyCost / 15
    }
  }

  @Callback(direct = true, doc = """function([dst: number, col: number, row: number, width: number, height: number, src: number, fromCol: number, fromRow: number]):boolean -- bitblt from buffer to screen. All parameters are optional. Writes to `dst` page in rectangle `x, y, width, height`, defaults to the bound screen and its viewport. Reads data from `src` page at `fx, fy`, default is the active page from position 1, 1""")
  def bitblt(context: Context, args: Arguments): Array[AnyRef] = {
    val dstIdx = args.optInteger(0, RESERVED_SCREEN_INDEX)
    screen(dstIdx, dst => {
      val col = args.optInteger(1, 1)
      val row = args.optInteger(2, 1)
      val w = args.optInteger(3, dst.getWidth)
      val h = args.optInteger(4, dst.getHeight)
      val srcIdx = args.optInteger(5, bufferIndex)
      screen(srcIdx, src => {
        val fromCol = args.optInteger(6, 1)
        val fromRow = args.optInteger(7, 1)

        var budgetCost: Double = determineBitbltBudgetCost(dst, src)
        val energyCost: Double = determineBitbltEnergyCost(dst)
        val tierCredit: Double = ((tier + 1) * .5)
        val overBudget: Double = budgetCost - tierCredit

        if (overBudget > 0) {
          if (budgetExhausted) { // we've thrown once before
            if (overBudget > tierCredit) { // we need even more pause than just a single tierCredit
              val pauseNeeded = overBudget - tierCredit
              val seconds: Double = (pauseNeeded / tierCredit) / 20
              context.pause(seconds)
            }
            budgetCost = 0 // remove the rest of the budget cost at this point
          } else {
            budgetExhausted = true
            throw new LimitReachedException()
          }
        }
        budgetExhausted = false

        if (resolveInvokeCosts(dstIdx, context, budgetCost, w * h, energyCost)) {
          if (dstIdx == srcIdx) {
            val tx = col - fromCol
            val ty = row - fromRow
            dst.copy(fromCol - 1, fromRow - 1, w, h, tx, ty)
            result(true)
          } else {
            // at least one of the two buffers is a gpu buffer
            component.GpuTextBuffer.bitblt(dst, col, row, w, h, src, fromRow, fromCol)
            result(true)
          }
        } else result(Unit, "not enough energy")
      })
    })
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
            s match {
              case oc: component.traits.VideoRamRasterizer => oc.removeAllBuffers()
              case _ =>
            }
          }
          else context.pause(0) // To discourage outputting "in realtime" to multiple screens using one GPU.
          result(true)
        })
      case _ => result(Unit, "not a screen")
    }
  }

  @Callback(direct = true, doc = """function():string -- Get the address of the screen the GPU is currently bound to.""")
  def getScreen(context: Context, args: Arguments): Array[AnyRef] = screen(RESERVED_SCREEN_INDEX, s => result(s.node.address))

  @Callback(direct = true, doc = """function():number, boolean -- Get the current background color and whether it's from the palette or not.""")
  def getBackground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getBackgroundColor, s.isBackgroundFromPalette))

  @Callback(direct = true, doc = """function(value:number[, palette:boolean]):number, number or nil -- Sets the background color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.""")
  def setBackground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    if (bufferIndex == RESERVED_SCREEN_INDEX) {
      context.consumeCallBudget(setBackgroundCosts(tier))
    }
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

  @Callback(direct = true, doc = """function():number, boolean -- Get the current foreground color and whether it's from the palette or not.""")
  def getForeground(context: Context, args: Arguments): Array[AnyRef] =
    screen(s => result(s.getForegroundColor, s.isForegroundFromPalette))

  @Callback(direct = true, doc = """function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.""")
  def setForeground(context: Context, args: Arguments): Array[AnyRef] = {
    val color = args.checkInteger(0)
    if (bufferIndex == RESERVED_SCREEN_INDEX) {
      context.consumeCallBudget(setForegroundCosts(tier))
    }
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

  @Callback(direct = true, doc = """function(index:number):number -- Get the palette color at the specified palette index.""")
  def getPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    screen(s => try result(s.getPaletteColor(index)) catch {
      case _: ArrayIndexOutOfBoundsException => throw new IllegalArgumentException("invalid palette index")
    })
  }

  @Callback(direct = true, doc = """function(index:number, color:number):number -- Set the palette color at the specified palette index. Returns the previous value.""")
  def setPaletteColor(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkInteger(0)
    val color = args.checkInteger(1)
    if (bufferIndex == RESERVED_SCREEN_INDEX) {
      context.consumeCallBudget(setPaletteColorCosts(tier))
      context.pause(0.1)
    }
    screen(s => try {
      val oldColor = s.getPaletteColor(index)
      s.setPaletteColor(index, color)
      result(oldColor)
    }
    catch {
      case _: ArrayIndexOutOfBoundsException => throw new IllegalArgumentException("invalid palette index")
    })
  }

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
    // maybe one day:
//    if (bufferIndex != RESERVED_SCREEN_INDEX && args.count() == 0) {
//      return screen {
//        case ram: GpuTextBuffer => {
//          val nbt = new CompoundNBT
//          ram.data.saveData(nbt)
//          result(nbt)
//        }
//      }
//    }
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
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val value = args.checkString(2)
    val vertical = args.optBoolean(3, false)

    screen(s => {
      if (resolveInvokeCosts(bufferIndex, context, setCosts(tier), value.length, Settings.get.gpuSetCost)) {
        s.set(x, y, value, vertical)
        result(true)
      } else result(Unit, "not enough energy")
    })
  }

  @Callback(direct = true, doc = """function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a portion of the screen from the specified location with the specified size by the specified translation.""")
  def copy(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = math.max(0, args.checkInteger(2))
    val h = math.max(0, args.checkInteger(3))
    val tx = args.checkInteger(4)
    val ty = args.checkInteger(5)
    screen(s => {
      if (resolveInvokeCosts(bufferIndex, context, copyCosts(tier), w * h, Settings.get.gpuCopyCost)) {
        s.copy(x, y, w, h, tx, ty)
        result(true)
      }
      else result(Unit, "not enough energy")
    })
  }

  @Callback(direct = true, doc = """function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a portion of the screen at the specified position with the specified size with the specified character.""")
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val x = args.checkInteger(0) - 1
    val y = args.checkInteger(1) - 1
    val w = math.max(0, args.checkInteger(2))
    val h = math.max(0, args.checkInteger(3))
    val value = args.checkString(4)
    if (value.length == 1) screen(s => {
      val c = value.charAt(0)
      val cost = if (c == ' ') Settings.get.gpuClearCost else Settings.get.gpuFillCost
      if (resolveInvokeCosts(bufferIndex, context, fillCosts(tier), w * h, cost)) {
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
    if (node.isNeighborOf(message.source)) {
      if (message.name == "computer.stopped" || message.name == "computer.started") {
        bufferIndex = RESERVED_SCREEN_INDEX
        removeAllBuffers()
      }
    }

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

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (screenInstance.isEmpty && screenAddress.fold(false)(_ == node.address)) {
      node.host match {
        case buffer: api.internal.TextBuffer =>
          screenInstance = Some(buffer)
        case _ => // Not the screen node we're looking for.
      }
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

  private val SCREEN_KEY: String = "screen"
  private val BUFFER_INDEX_KEY: String = "bufferIndex"
  private val VIDEO_RAM_KEY: String = "videoRam"
  private final val NBT_PAGES: String = "pages"
  private final val NBT_PAGE_IDX: String = "page_idx"
  private final val NBT_PAGE_DATA: String = "page_data"
  private val COMPOUND_ID = (new CompoundNBT).getId

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)

    if (nbt.contains(SCREEN_KEY)) {
      nbt.getString(SCREEN_KEY) match {
        case screen: String if !screen.isEmpty => screenAddress = Some(screen)
        case _ => screenAddress = None
      }
      screenInstance = None
    }

    if (nbt.contains(BUFFER_INDEX_KEY)) {
      bufferIndex = nbt.getInt(BUFFER_INDEX_KEY)
    }

    removeAllBuffers() // JUST in case
    if (nbt.contains(VIDEO_RAM_KEY)) {
      val videoRamNbt = nbt.getCompound(VIDEO_RAM_KEY)
      val nbtPages = videoRamNbt.getList(NBT_PAGES, COMPOUND_ID)
      for (i <- 0 until nbtPages.size) {
        val nbtPage = nbtPages.getCompound(i)
        val idx: Int = nbtPage.getInt(NBT_PAGE_IDX)
        val data = nbtPage.getCompound(NBT_PAGE_DATA)
        loadBuffer(node.address, idx, data)
      }
    }
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)

    if (screenAddress.isDefined) {
      nbt.putString(SCREEN_KEY, screenAddress.get)
    }

    nbt.putInt(BUFFER_INDEX_KEY, bufferIndex)

    val videoRamNbt = new CompoundNBT
    val nbtPages = new ListNBT

    val indexes = bufferIndexes()
    for (idx: Int <- indexes) {
      getBuffer(idx) match {
        case Some(page) => {
          val nbtPage = new CompoundNBT
          nbtPage.putInt(NBT_PAGE_IDX, idx)
          val data = new CompoundNBT
          page.data.saveData(data)
          nbtPage.put(NBT_PAGE_DATA, data)
          nbtPages.add(nbtPage)
        }
        case _ => // ignore
      }
    }
    videoRamNbt.put(NBT_PAGES, nbtPages)
    nbt.put(VIDEO_RAM_KEY, videoRamNbt)
  }
}
