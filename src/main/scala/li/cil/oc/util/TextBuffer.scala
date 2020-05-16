package li.cil.oc.util

import li.cil.oc.Settings
import li.cil.oc.api
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
class TextBuffer(var width: Int, var height: Int, initialFormat: PackedColor.ColorFormat) {
  def this(size: (Int, Int), format: PackedColor.ColorFormat) = this(size._1, size._2, format)

  private var _format = initialFormat

  private var _foreground = PackedColor.Color(0xFFFFFF)

  private var _background = PackedColor.Color(0x000000)

  private var packed = PackedColor.pack(_foreground, _background, _format)

  def foreground = _foreground

  def foreground_=(value: PackedColor.Color) = {
    format.validate(value)
    _foreground = value
    packed = PackedColor.pack(_foreground, _background, _format)
    this
  }

  def background = _background

  def background_=(value: PackedColor.Color) = {
    format.validate(value)
    _background = value
    packed = PackedColor.pack(_foreground, _background, _format)
    this
  }

  def format = _format

  def format_=(value: PackedColor.ColorFormat) = {
    if (format.depth != value.depth) {
      for (row <- 0 until height) {
        val rowColor = color(row)
        for (col <- 0 until width) {
          val packed = rowColor(col)
          val fg = PackedColor.Color(PackedColor.unpackForeground(packed, _format))
          val bg = PackedColor.Color(PackedColor.unpackBackground(packed, _format))
          rowColor(col) = PackedColor.pack(fg, bg, value)
        }
      }
      _format = value
      packed = PackedColor.pack(_foreground, _background, _format)
      true
    }
    else false
  }

  var color = Array.fill(height, width)(packed)

  var buffer = Array.fill(height, width)(' ')
  
  var dirty =  Array.fill(height, width)(true)

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
      dirty = Array.fill(h, w)(true)
      width = w
      height = h
      true
    }
    else false
  }

  /** Get the char at the specified index. */
  def get(col: Int, row: Int) = {
    if (col < 0 || col >= width || row < 0 || row >= height)
      throw new IndexOutOfBoundsException()
    else buffer(row)(col)
  }

  /** String based fill starting at a specified location. */
  def set(col: Int, row: Int, s: String, vertical: Boolean): Boolean =
    if (vertical) {
      if (col < 0 || col >= width) false
      else {
        var changed = false
        for (y <- row until math.min(row + s.length, height)) if (y >= 0) {
          val line = buffer(y)
          val lineColor = color(y)
          val lineDirty = dirty(y)
          val c = s(y - row)
          changed = changed || (line(col) != c) || (lineColor(col) != packed)
          setChar(line, lineColor, lineDirty, col, c)
        }
        changed
      }
    }
    else {
      if (row < 0 || row >= height) false
      else {
        var changed = false
        val line = buffer(row)
        val lineColor = color(row)
        val lineDirty = dirty(row)
        var bx = math.max(col, 0)
        for (x <- bx until math.min(col + s.length, width) if bx < line.length) {
          val c = s(x - col)
          changed = changed || (line(bx) != c) || (lineColor(bx) != packed)
          setChar(line, lineColor, lineDirty, bx, c)
          bx += math.max(1, FontUtils.wcwidth(c))
        }
        changed
      }
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
      val lineDirty = dirty(y)
      var bx = math.max(col, 0)
      for (x <- bx until math.min(col + w, width) if bx < line.length) {
        changed = changed || (line(bx) != c) || (lineColor(bx) != packed)
        setChar(line, lineColor, lineDirty, bx, c)
        bx += math.max(1, FontUtils.wcwidth(c))
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
    val (dx0, dx1) = (math.max(0, math.min(width - 1, col + tx + w - 1)), math.max(0, math.min(width, col + tx))) match {
      case dx if tx > 0 => dx
      case dx => dx.swap
    }
    val left_edge = math.min(dx0, dx1) - 1
    if (left_edge >= width - 1) return false // no work
    val (dy0, dy1) = (math.max(0, math.min(height - 1, row + ty + h - 1)), math.max(0, math.min(height, row + ty))) match {
      case dy if ty > 0 => dy
      case dy => dy.swap
    }
    val (sx, sy) = (if (tx > 0) -1 else 1, if (ty > 0) -1 else 1)
    // Copy values to destination rectangle if there source is valid.
    var changed = false
    for (ny <- dy0 to dy1 by sy) {
      val nl = buffer(ny)
      val nc = color(ny)
      val nd = dirty(ny)
      ny - ty match {
        case oy if oy >= 0 && oy < height =>
          val ol = buffer(oy)
          val oc = color(oy)
          for (nx <- dx0 to dx1 by sx) nx - tx match {
            case ox if ox >= 0 && ox < width =>
              changed = changed || (nl(nx) != ol(ox)) || (nc(nx) != oc(ox))
              nl(nx) = ol(ox)
              nc(nx) = oc(ox)
              nd(nx) = true
              for (offset <- 1 until FontUtils.wcwidth(nl(nx))) {
                nl(nx + offset) = ' '
                nc(nx + offset) = oc(nx)
                nd(nx + offset) = true
              }
            case _ => /* Got no source column. */
          }
          // any wide chars along the left edge of the target rectangle need to be cleared
          // don't change their colors
          if (left_edge >= 0 && FontUtils.wcwidth(nl(left_edge)) > 1) {
            nl(left_edge) = ' '
            nd(left_edge) = true
            changed = true
          }
        case _ => /* Got no source row. */
      }
    }
    changed
  }

  private def setChar(line: Array[Char], lineColor: Array[Short], lineDirty: Array[Boolean], x: Int, c: Char) {
    if (FontUtils.wcwidth(c) > 1 && x >= line.length - 1) {
      // Don't allow setting wide chars in right-most col.
      return
    }
    line(x) = c
    lineColor(x) = packed
    lineDirty(x) = true
    for (x1 <- x + 1 until x + FontUtils.wcwidth(c)) {
      line(x1) = ' '
      lineColor(x1) = packed
      lineDirty(x1) = true
    }
    if (x > 0 && FontUtils.wcwidth(line(x - 1)) > 1) {
      // remove previous wide char (but don't change its color)
      line(x - 1) = ' '
      lineDirty(x - 1) = true
    }
  }

  def load(nbt: NBTTagCompound): Unit = {
    val maxResolution = math.max(Settings.screenResolutionsByTier.last._1, Settings.screenResolutionsByTier.last._2)
    val w = nbt.getInteger("width") min maxResolution max 1
    val h = nbt.getInteger("height") min maxResolution max 1
    size = (w, h)

    val b = nbt.getTagList("buffer", NBT.TAG_STRING)
    for (i <- 0 until math.min(h, b.tagCount)) {
      val value = b.getStringTagAt(i)
      System.arraycopy(value.toCharArray, 0, buffer(i), 0, math.min(value.length, buffer(i).length))
    }

    val depth = api.internal.TextBuffer.ColorDepth.values.apply(nbt.getInteger("depth") min (api.internal.TextBuffer.ColorDepth.values.length - 1) max 0)
    _format = PackedColor.Depth.format(depth)
    _format.load(nbt)
    foreground = PackedColor.Color(nbt.getInteger("foreground"), nbt.getBoolean("foregroundIsPalette"))
    background = PackedColor.Color(nbt.getInteger("background"), nbt.getBoolean("backgroundIsPalette"))

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

  def save(nbt: NBTTagCompound): Unit = {
    nbt.setInteger("width", width)
    nbt.setInteger("height", height)

    val b = new NBTTagList()
    for (i <- 0 until height) {
      b.appendTag(new NBTTagString(String.valueOf(buffer(i))))
    }
    nbt.setTag("buffer", b)

    nbt.setInteger("depth", _format.depth.ordinal)
    _format.save(nbt)
    nbt.setInteger("foreground", _foreground.value)
    nbt.setBoolean("foregroundIsPalette", _foreground.isPalette)
    nbt.setInteger("background", _background.value)
    nbt.setBoolean("backgroundIsPalette", _background.isPalette)

    nbt.setTag("color", new NBTTagIntArray(color.flatten.map(_.toInt)))
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