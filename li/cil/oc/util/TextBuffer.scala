package li.cil.oc.util

import net.minecraft.nbt.{NBTTagIntArray, NBTTagCompound, NBTTagList, NBTTagString}

/**
 * This stores chars in a 2D-Array and provides some manipulation functions.
 *
 * The main purpose of this is to allow moving most implementation detail to
 * the Lua side while keeping bandwidth costs low and still allowing for
 * relatively fast updates, given a smart algorithm (using copy()/fill()
 * instead of set()ing everything).
 */
class TextBuffer(var width: Int, var height: Int, val depth: PackedColor.Depth.Value) {
  def this(size: (Int, Int), depth: PackedColor.Depth.Value) = this(size._1, size._2, depth)

  private var foreground_ = 0xFFFFFF

  private var background_ = 0x000000

  private var packed = PackedColor.pack(foreground_, background_, depth)

  def foreground = foreground_

  def foreground_=(value: Int) = {
    foreground_ = value
    packed = PackedColor.pack(foreground_, background_, depth)
  }

  def background = background_

  def background_=(value: Int) = {
    background_ = value
    packed = PackedColor.pack(foreground_, background_, depth)
  }

  var color = Array.fill(height, width)(packed)

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
  def size_=(value: (Int, Int)): Boolean = {
    val (iw, ih) = value
    val (w, h) = (iw max 1, ih max 1)
    if (width != w || height != h) {
      val newBuffer = Array.fill(h, w)(' ')
      val newColor = Array.fill(h, w)(packed)
      (0 until (h min height)).foreach(y => {
        Array.copy(buffer(y), 0, newBuffer(y), 0, w min width)
        Array.copy(color(y), 0, newColor(y), 0, w min width)
      })
      buffer = newBuffer
      color = newColor
      width = w
      height = h
      true
    }
    else false
  }

  /** Get the char at the specified index. */
  def get(col: Int, row: Int) = buffer(row)(col)

  /** String based fill starting at a specified location. */
  def set(col: Int, row: Int, s: String): Boolean =
    if (row < 0 || row >= height) false
    else {
      var changed = false
      val line = buffer(row)
      val lineColor = color(row)
      for (x <- col until ((col + s.length) min width)) if (x >= 0) {
        val c = s(x - col)
        changed = changed || (line(x) != c) || (lineColor(x) != packed)
        line(x) = c
        lineColor(x) = packed
      }
      changed
    }

  /** Fills an area of the buffer with the specified character. */
  def fill(col: Int, row: Int, w: Int, h: Int, c: Char): Boolean = {
    // Anything to do at all?
    if (w <= 0 || h <= 0) return false
    if (col + w < 0 || row + h < 0 || col >= width || row >= height) return false
    var changed = false
    for (y <- (row max 0) until ((row + h) min height)) {
      val line = buffer(y)
      val lineColor = color(y)
      for (x <- (col max 0) until ((col + w) min width)) {
        changed = changed || (line(x) != c) || (lineColor(x) != packed)
        line(x) = c
        lineColor(x) = packed
      }
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
      case dx if tx > 0 => dx
      case dx => dx.swap
    }
    val (dy0, dy1) = ((row + ty + h - 1) max 0 min (height - 1), (row + ty) max 0 min height) match {
      case dy if ty > 0 => dy
      case dy => dy.swap
    }
    val (sx, sy) = (if (tx > 0) -1 else 1, if (ty > 0) -1 else 1)
    // Copy values to destination rectangle if there source is valid.
    var changed = false
    for (ny <- dy0 to dy1 by sy) {
      val nl = buffer(ny)
      val nc = color(ny)
      ny - ty match {
        case oy if oy >= 0 && oy < height =>
          val ol = buffer(oy)
          val oc = color(oy)
          for (nx <- dx0 to dx1 by sx) nx - tx match {
            case ox if ox >= 0 && ox < width => {
              changed = changed || (nl(nx) != ol(ox)) || (nc(nx) != oc(ox))
              nl(nx) = ol(ox)
              nc(nx) = oc(ox)
            }
            case _ => /* Got no source column. */
          }
        case _ => /* Got no source row. */
      }
    }
    changed
  }

  def readFromNBT(nbt: NBTTagCompound): Unit = {
    val w = nbt.getInteger("width")
    val h = nbt.getInteger("height")
    size = (w, h)
    val b = nbt.getTagList("buffer")
    for (i <- 0 until (h min b.tagCount)) {
      val line = b.tagAt(i).asInstanceOf[NBTTagString].data
      set(0, i, line)
    }
    val c = nbt.getTagList("color")
    for (i <- 0 until (h min c.tagCount)) {
      val line = c.tagAt(i).asInstanceOf[NBTTagIntArray].intArray
      Array.copy(line, 0, color(i), 0, line.length min width)
    }
  }

  def writeToNBT(nbt: NBTTagCompound): Unit = {
    nbt.setInteger("width", width)
    nbt.setInteger("height", height)
    val b = new NBTTagList()
    for (i <- 0 until height) {
      b.appendTag(new NBTTagString(null, String.valueOf(buffer(i))))
    }
    nbt.setTag("buffer", b)
    val c = new NBTTagList()
    for (i <- 0 until height) {
      c.appendTag(new NBTTagIntArray(null, color(i)))
    }
    nbt.setTag("color", c)
  }

  override def toString = {
    val b = StringBuilder.newBuilder
    if (buffer.length > 0) {
      b.appendAll(buffer(0))
      for (y <- 1 until height) {
        b.append('\n').appendAll(buffer(y))
      }
    }
    b.toString()
  }
}