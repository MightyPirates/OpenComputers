package li.cil.oc.util

import li.cil.oc.Settings
import net.minecraft.nbt._
import net.minecraftforge.common.util.Constants.NBT

/**
 * This stores chars in a 2D-Array and provides some manipulation functions.
 *
 * The main purpose of this is to allow moving most implementation detail to
 * the Lua side while keeping bandwidth costs low and still allowing for
 * relatively fast updates, given a smart algorithm (using copy()/fill()
 * instead of set()ing everything).
 */
class TextBuffer(var width: Int, var height: Int, initialDepth: PackedColor.Depth.Value) {
  def this(size: (Int, Int), depth: PackedColor.Depth.Value) = this(size._1, size._2, depth)

  private var _depth = initialDepth

  private var _foreground = 0xFFFFFF

  private var _background = 0x000000

  private var packed = PackedColor.pack(_foreground, _background, _depth)

  def foreground = _foreground

  def foreground_=(value: Int) = {
    _foreground = value
    packed = PackedColor.pack(_foreground, _background, _depth)
  }

  def background = _background

  def background_=(value: Int) = {
    _background = value
    packed = PackedColor.pack(_foreground, _background, _depth)
  }

  def depth = _depth

  def depth_=(value: PackedColor.Depth.Value) = {
    if (depth != value) {
      for (row <- 0 until height) {
        val rowColor = color(row)
        for (col <- 0 until width) {
          val packed = rowColor(col)
          val fg = PackedColor.unpackForeground(packed, _depth)
          val bg = PackedColor.unpackBackground(packed, _depth)
          rowColor(col) = PackedColor.pack(fg, bg, value)
        }
      }
      _depth = value
      packed = PackedColor.pack(_foreground, _background, _depth)
      true
    }
    else false
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
    val (w, h) = (math.max(iw, 1), math.max(ih, 1))
    if (width != w || height != h) {
      val newBuffer = Array.fill(h, w)(' ')
      val newColor = Array.fill(h, w)(packed)
      (0 until math.min(h, height)).foreach(y => {
        Array.copy(buffer(y), 0, newBuffer(y), 0, math.min(w, width))
        Array.copy(color(y), 0, newColor(y), 0, math.min(w, width))
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
      for (x <- col until math.min(col + s.length, width)) if (x >= 0) {
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
    for (y <- math.max(row, 0) until math.min(row + h, height)) {
      val line = buffer(y)
      val lineColor = color(y)
      for (x <- math.max(col, 0) until math.min(col + w, width)) {
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
    val (dx0, dx1) = (math.max(col + tx + w - 1, math.min(0, width - 1)), math.max(col + tx, math.min(0, width))) match {
      case dx if tx > 0 => dx
      case dx => dx.swap
    }
    val (dy0, dy1) = (math.max(row + ty + h - 1, math.min(0, height - 1)), math.max(row + ty, math.min(0, height))) match {
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
            case ox if ox >= 0 && ox < width =>
              changed = changed || (nl(nx) != ol(ox)) || (nc(nx) != oc(ox))
              nl(nx) = ol(ox)
              nc(nx) = oc(ox)
            case _ => /* Got no source column. */
          }
        case _ => /* Got no source row. */
      }
    }
    changed
  }

  def load(nbt: NBTTagCompound): Unit = {
    val w = nbt.getInteger("width") max 1 min Settings.screenResolutionsByTier(2)._1
    val h = nbt.getInteger("height") max 1 min Settings.screenResolutionsByTier(2)._2
    size = (w, h)

    val b = nbt.getTagList("buffer", NBT.TAG_STRING)
    for (i <- 0 until math.min(h, b.tagCount)) {
      set(0, i, b.getStringTagAt(i))
    }

    _depth = PackedColor.Depth(nbt.getInteger("depth") max 0 min PackedColor.Depth.maxId)
    foreground = nbt.getInteger("foreground")
    background = nbt.getInteger("background")

    // For upgrading from 1.6 - was tag list of short before.
    if (nbt.hasKey("color", NBT.TAG_INT_ARRAY)) {
      val c = nbt.getIntArray("color")
      for (i <- 0 until h) {
        val rowColor = color(i)
        for (j <- 0 until w) {
          val index = j + i * w
          if (index < c.length) {
            rowColor(j) = c(index).toShort
          }
        }
      }
    }
  }

  def save(nbt: NBTTagCompound): Unit = {
    nbt.setInteger("width", width)
    nbt.setInteger("height", height)

    val b = new NBTTagList()
    for (i <- 0 until height) {
      b.appendTag(new NBTTagString(String.valueOf(buffer(i))))
    }
    nbt.setTag("buffer", b)

    nbt.setInteger("depth", _depth.id)
    nbt.setInteger("foreground", _foreground)
    nbt.setInteger("background", _background)

    nbt.setTag("color", new NBTTagIntArray(color.flatten.map(_.toInt).toArray))
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