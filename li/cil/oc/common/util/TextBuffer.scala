package li.cil.oc.common.util

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

import net.minecraft.nbt.NBTTagString

/** This stores chars in a 2D-Array and provides some manipulation functions. */
class TextBuffer(var width: Int, var height: Int) {
  var buffer = Array.fill(height, width)(' ')

  def size = (width, height)

  def size_=(value: (Int, Int)): Unit = {
    val (w, h) = value
    val nbuffer = Array.fill(h, w)(' ')
    (0 until (h min height)) foreach {
      y => Array.copy(buffer(y), 0, nbuffer(y), 0, w min width)
    }
    buffer = nbuffer
    width = w
    height = h
  }

  def set(col: Int, row: Int, s: String): Unit =
    for (i <- col until ((col + s.length) min width))
      buffer(row)(i) = s(i - col)

  def fill(x: Int, y: Int, w: Int, h: Int, c: Char): Unit =
    for (y <- (y max 0 min height) until ((y + h) max 0 min height))
      for (x <- (x max 0 min width) until ((x + w) max 0 min width))
        buffer(y)(x) = c

  /** Copies a portion of the buffer. */
  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {
    // Anything to do at all?
    if (w <= 0 || h <= 0) return
    if (tx == 0 && ty == 0) return
    // Loop over the target rectangle, starting from the directions away from
    // the source rectangle and copy the data. This way we ensure we don't
    // overwrite anything we still need to copy.
    val (dx0, dx1) = ((col + tx + w - 1) max 0 min (width - 1), (col + tx) max 0 min width) match {
      case destx if tx > 0 => destx
      case destx => destx.swap
    }
    val (dy0, dy1) = ((row + ty + h - 1) max 0 min (height - 1), (row + ty) max 0 min height) match {
      case desty if (ty > 0) => desty
      case desty => desty.swap
    }
    val (sx, sy) = ((if (tx > 0) -1 else 1), (if (ty > 0) -1 else 1))
    // Copy values to destination rectangle if there source is valid.
    for (ny <- dy0 to dy1 by sy) (ny - ty) match {
      case oy if oy >= 0 && oy < height =>
        for (nx <- dx0 to dx1 by sx) (nx - tx) match {
          case ox if ox >= 0 && ox < width => buffer(ny)(nx) = buffer(oy)(ox)
          case _ => /* Got no source column. */
        }
      case _ => /* Got no source row. */
    }
  }

  def readFromNBT(nbt: NBTTagCompound): Unit = {
    val w = nbt.getInteger("width")
    val h = nbt.getInteger("height")
    size = (w, h)
    val b = nbt.getTagList("buffer")
    for (i <- 0 until (h min b.tagCount())) {
      set(0, i, b.tagAt(i).asInstanceOf[NBTTagString].data)
    }
  }

  def writeToNBT(nbt: NBTTagCompound): Unit = {
    nbt.setInteger("width", width)
    nbt.setInteger("height", height)
    val b = new NBTTagList("buffer")
    for (i <- 0 until height) {
      b.appendTag(new NBTTagString(null, String.valueOf(buffer(i))))
    }
  }

  override def toString = {
    val b = StringBuilder.newBuilder
    if (buffer.length > 0) {
      b.appendAll(buffer(0))
      for (y <- 1 until height) {
        b.append('\n').appendAll(buffer(y))
      }
    }
    b.toString
  }
}