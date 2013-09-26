package li.cil.oc.common.components

import li.cil.oc.common.util.TextBuffer
import net.minecraft.nbt.NBTTagCompound

class Screen(val owner: IScreenEnvironment) {
  val supportedResolutions = List((40, 24), (80, 24))

  private val buffer = new TextBuffer(80, 24)

  def text = buffer.toString

  def lines = buffer.buffer

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) =
    if (supportedResolutions.contains(value) && (buffer.size = value)) {
      val (w, h) = value
      owner.onScreenResolutionChange(w, h)
      true
    }
    else false

  def set(col: Int, row: Int, s: String) = if (col < buffer.width && (col >= 0 || -col < s.length)) {
    // Make sure the string isn't longer than it needs to be, in particular to
    // avoid sending too much data to our clients.
    val (x, truncated) =
      if (col < 0) (0, s.substring(-col))
      else (col, s.substring(0, s.length min buffer.width))
    if (buffer.set(x, row, truncated))
      owner.onScreenSet(x, row, truncated)
  }

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char) =
    if (buffer.fill(col, row, w, h, c))
      owner.onScreenFill(col, row, w, h, c)

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) =
    if (buffer.copy(col, row, w, h, tx, ty))
      owner.onScreenCopy(col, row, w, h, tx, ty)

  def load(nbt: NBTTagCompound) = {
    buffer.readFromNBT(nbt.getCompoundTag("buffer"))
  }

  def save(nbt: NBTTagCompound) = {
    val nbtBuffer = new NBTTagCompound
    buffer.writeToNBT(nbtBuffer)
    nbt.setCompoundTag("buffer", nbtBuffer)
  }
}