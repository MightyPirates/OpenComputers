package li.cil.oc.common.component

import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component
import li.cil.oc.common.tileentity.TileEntity
import li.cil.oc.util.{Persistable, PackedColor, TextBuffer}
import li.cil.oc.{api, Config}
import net.minecraft.nbt.NBTTagCompound

class Buffer(val owner: Buffer.Environment) extends Persistable {
  val buffer = new TextBuffer(maxResolution, maxDepth)

  def maxResolution = Config.screenResolutionsByTier(owner.tier)

  def maxDepth = Config.screenDepthsByTier(owner.tier)

  // ----------------------------------------------------------------------- //

  def text = buffer.toString

  def lines = buffer.buffer

  def colors = buffer.color

  def depth = buffer.depth

  def depth_=(value: PackedColor.Depth.Value) = {
    if (value > maxDepth)
      throw new IllegalArgumentException("unsupported depth")
    if (buffer.depth = value) {
      owner.onScreenDepthChange(value)
      true
    }
    else false
  }

  def foreground = buffer.foreground

  def foreground_=(value: Int) = {
    if (buffer.foreground != value) {
      val result = buffer.foreground
      buffer.foreground = value
      owner.onScreenColorChange(foreground, background)
      result
    }
    else value
  }

  def background = buffer.background

  def background_=(value: Int) = {
    if (buffer.background != value) {
      val result = buffer.background
      buffer.background = value
      owner.onScreenColorChange(foreground, background)
      result
    }
    else value
  }

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) = {
    val (w, h) = value
    val (mw, mh) = maxResolution
    if (w < 1 || w > mw || h < 1 || h > mh)
      throw new IllegalArgumentException("unsupported resolution")
    if (buffer.size = value) {
      owner.onScreenResolutionChange(w, h)
      true
    }
    else false
  }

  def get(col: Int, row: Int) = buffer.get(col, row)

  def set(col: Int, row: Int, s: String) = if (col < buffer.width && (col >= 0 || -col < s.length)) {
    // Make sure the string isn't longer than it needs to be, in particular to
    // avoid sending too much data to our clients.
    val (x, truncated) =
      if (col < 0) (0, s.substring(-col))
      else (col, s.substring(0, s.length min buffer.width))
    if (consumePower(truncated.length, Config.screenSetCost))
      if (buffer.set(x, row, truncated))
        owner.onScreenSet(x, row, truncated)
  }

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char) =
    if (consumePower(w * h, if (c == ' ') Config.screenClearCost else Config.screenFillCost))
      if (buffer.fill(col, row, w, h, c))
        owner.onScreenFill(col, row, w, h, c)

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) =
    if (consumePower(w * h, Config.screenCopyCost))
      if (buffer.copy(col, row, w, h, tx, ty))
        owner.onScreenCopy(col, row, w, h, tx, ty)

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    buffer.load(nbt.getCompoundTag(Config.namespace + "screen"))
  }

  override def save(nbt: NBTTagCompound) = {
    val screenNbt = new NBTTagCompound
    buffer.save(screenNbt)
    nbt.setCompoundTag(Config.namespace + "screen", screenNbt)
  }

  // ----------------------------------------------------------------------- //

  private def consumePower(n: Double, cost: Double) = owner.node == null || {
    owner.node.changeBuffer(-n * cost * (1 + owner.size * Config.screenMultiCostScale))
  }

}

object Buffer {

  trait Environment extends TileEntity with api.network.Environment with Persistable {
    val node = api.Network.newNode(this, Visibility.Network).
      withComponent("screen").
      withConnector().
      create()

    final val instance = new component.Buffer(this)

    def tier: Int

    def size: Int

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) = {
      super.load(nbt)
      instance.load(nbt)
    }

    override def save(nbt: NBTTagCompound) = {
      super.save(nbt)
      instance.save(nbt)
    }

    // ----------------------------------------------------------------------- //

    def onScreenColorChange(foreground: Int, background: Int) {}

    def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {}

    def onScreenDepthChange(depth: PackedColor.Depth.Value) {}

    def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {}

    def onScreenResolutionChange(w: Int, h: Int) {
      if (node != null)
        node.sendToReachable("computer.signal", "screen_resized", Int.box(w), Int.box(h))
    }

    def onScreenSet(col: Int, row: Int, s: String) {}
  }

}