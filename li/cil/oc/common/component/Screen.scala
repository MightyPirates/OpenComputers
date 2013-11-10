package li.cil.oc.common.component

import li.cil.oc.api.Persistable
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.{tileentity, component}
import li.cil.oc.util.{PackedColor, TextBuffer}
import li.cil.oc.{Config, util, api}
import net.minecraft.nbt.NBTTagCompound

class Screen(val owner: Screen.Environment, val maxResolution: (Int, Int)) extends Persistable {
  private val buffer = new TextBuffer(maxResolution, PackedColor.Depth.OneBit)

  // ----------------------------------------------------------------------- //

  def text = buffer.toString

  def lines = buffer.buffer

  def colors = buffer.color

  def depth = buffer.depth

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) = {
    val (w, h) = value
    val (mw, mh) = maxResolution
    if (w <= mw && h <= mh && (buffer.size = value)) {
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
    buffer.readFromNBT(nbt.getCompoundTag("oc.screen"))
  }

  override def save(nbt: NBTTagCompound) = {
    val screenNbt = new NBTTagCompound
    buffer.writeToNBT(screenNbt)
    nbt.setCompoundTag("oc.screen", screenNbt)
  }

  // ----------------------------------------------------------------------- //

  private def consumePower(n: Double, cost: Double) =
    owner.node == null || owner.node.changeBuffer(-n * cost)
}

object Screen {

  trait Environment extends tileentity.Environment with util.Persistable {
    val node = api.Network.newNode(this, Visibility.Network).
      withComponent("screen").
      withConnector(Config.bufferScreen * (tier + 1)).
      create()

    final val instance = new component.Screen(this, Config.screenResolutionsByTier(tier))

    protected def tier: Int

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) = {
      super.load(nbt)
      if (node != null) node.load(nbt)
      instance.load(nbt)
    }

    override def save(nbt: NBTTagCompound) = {
      super.save(nbt)
      if (node != null) node.save(nbt)
      instance.save(nbt)
    }

    // ----------------------------------------------------------------------- //

    def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {}

    def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {}

    def onScreenResolutionChange(w: Int, h: Int) {
      if (node != null)
        node.sendToReachable("computer.signal", "screen_resized", Int.box(w), Int.box(h))
    }

    def onScreenSet(col: Int, row: Int, s: String) {}
  }

}