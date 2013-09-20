package li.cil.oc.common.util

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

import net.minecraft.nbt.NBTTagString

/**
 * This stores chars in a 2D-Array and provides some manipulation functions.
 *
 * The main purpose of this is to allow moving most implementation detail to
 * the Lua side while keeping bandwidth costs low and still allowing for
 * relatively fast updates, given a smart algorithm (using copy()/fill()
 * instead of set()ing everything).
 */
class TextBuffer(var width: Int, var height: Int) {
  var buffer = Array.fill(height, width)(' ')

  /** The current buffer size in columns by rows. */
  def size = (width, height)

  /**
   * Set the new buffer size, returns true if the size changed.
   *
   * This will perform a proper resize as required, keeping as much of the
   * buffer valid as possible if the size decreases, i.e. only data outside the
   * new buffer size will be truncated, all data still inside will be copied.
   */
  def size_=(value: (Int, Int)): Boolean = if (size != value) {
    val (w, h) = value
    val nbuffer = Array.fill(h, w)(' ')
    (0 until (h min height)) foreach {
      y => Array.copy(buffer(y), 0, nbuffer(y), 0, w min width)
    }
    buffer = nbuffer
    width = w
    height = h
    true
  }
  else false

  /**
   * String based fill starting at a specified location.
   *
   * Note that this will ignore any newline characters to ensure only the
   * specified row is changed and thereby limiting the size of the packet that
   * has to be sent to clients to notify them of the change. In fact, this will
   * ignore any and all control characters as according to Java's Character's
   * 'isISOControl' property (0-31 and 127-159).
   */
  def set(col: Int, row: Int, s: String): Boolean = {
    var changed = false
    for (i <- col until ((col + s.length) min width)) {
      s(i - col) match {
        case c if c.isControl => // Ignore.
        case c =>
          changed = changed || (buffer(row)(i) != c)
          buffer(row)(i) = c
      }
    }
    changed
  }

  /**
   * Fills an area of the buffer with the specified character.
   *
   * Note that like set() this will ignore control characters (it will do
   * nothing in that case).
   */
  def fill(x: Int, y: Int, w: Int, h: Int, c: Char): Boolean =
    if (c.isControl) false
    else {
      var changed = false
      for (y <- (y max 0 min height) until ((y + h) max 0 min height))
        for (x <- (x max 0 min width) until ((x + w) max 0 min width)) {
          changed = changed || (buffer(y)(x) != c)
          buffer(y)(x) = c
        }
      changed
    }

  /** Copies a portion of the buffer. */
  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Boolean = {
    // Anything to do at all?
    if (w <= 0 || h <= 0) return false
    if (tx == 0 && ty == 0) return false
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
    var changed = false
    for (ny <- dy0 to dy1 by sy) (ny - ty) match {
      case oy if oy >= 0 && oy < height =>
        for (nx <- dx0 to dx1 by sx) (nx - tx) match {
          case ox if ox >= 0 && ox < width => {
            changed = changed || (buffer(ny)(nx) != buffer(oy)(ox))
            buffer(ny)(nx) = buffer(oy)(ox)
          }
          case _ => /* Got no source column. */
        }
      case _ => /* Got no source row. */
    }
    changed
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
    val b = new NBTTagList
    for (i <- 0 until height) {
      b.appendTag(new NBTTagString("", String.valueOf(buffer(i))))
    }
    nbt.setTag("buffer", b)
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