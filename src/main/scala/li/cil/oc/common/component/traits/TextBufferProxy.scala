package li.cil.oc.common.component.traits

import li.cil.oc.util
import li.cil.oc.api
import li.cil.oc.api.internal.TextBuffer
import li.cil.oc.util.{ExtendedUnicodeHelper, PackedColor}

trait TextBufferProxy extends api.internal.TextBuffer {
  def data: util.TextBuffer

  override def getWidth: Int = data.width

  override def getHeight: Int = data.height

  override def setColorDepth(depth: api.internal.TextBuffer.ColorDepth): Boolean = {
    if (depth.ordinal > getMaximumColorDepth.ordinal)
      throw new IllegalArgumentException("unsupported depth")
    data.format = PackedColor.Depth.format(depth)
  }

  override def getColorDepth: TextBuffer.ColorDepth = data.format.depth

  def onBufferPaletteChange(index: Int): Unit = {}

  override def setPaletteColor(index: Int, color: Int): Unit = data.format match {
    case palette: PackedColor.MutablePaletteFormat =>
      palette(index) = color
      onBufferPaletteChange(index)
    case _ => throw new Exception("palette not available")
  }

  override def getPaletteColor(index: Int): Int = data.format match {
    case palette: PackedColor.MutablePaletteFormat => palette(index)
    case _ => throw new Exception("palette not available")
  }

  def onBufferColorChange(): Unit = {}

  override def setForegroundColor(color: Int): Unit = setForegroundColor(color, isFromPalette = false)

  override def setForegroundColor(color: Int, isFromPalette: Boolean) {
    val value = PackedColor.Color(color, isFromPalette)
    if (data.foreground != value) {
      data.foreground = value
      onBufferColorChange()
    }
  }

  override def getForegroundColor: Int = data.foreground.value

  override def isForegroundFromPalette: Boolean = data.foreground.isPalette

  override def setBackgroundColor(color: Int): Unit = setBackgroundColor(color, isFromPalette = false)

  override def setBackgroundColor(color: Int, isFromPalette: Boolean) {
    val value = PackedColor.Color(color, isFromPalette)
    if (data.background != value) {
      data.background = value
      onBufferColorChange()
    }
  }

  override def getBackgroundColor: Int = data.background.value

  override def isBackgroundFromPalette: Boolean = data.background.isPalette

  def onBufferCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit = {}

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit =
    if (data.copy(col, row, w, h, tx, ty))
      onBufferCopy(col, row, w, h, tx, ty)

  def onBufferFill(col: Int, row: Int, w: Int, h: Int, c: Int): Unit = {}

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char): Unit =
    fill(col, row, w, h, c.toInt)

  def fill(col: Int, row: Int, w: Int, h: Int, c: Int): Unit =
    if (data.fill(col, row, w, h, c))
      onBufferFill(col, row, w, h, c)

  def onBufferSet(col: Int, row: Int, s: String, vertical: Boolean): Unit = {}

  private def truncate(s: String, sLength: Int, leftOffset: Int, maxWidth: Int): String = {
    val subFrom = s.offsetByCodePoints(0, leftOffset)
    val width = math.min(sLength, maxWidth)
    if (width <= 0) ""
    else if ((sLength - leftOffset) <= width) s
    else s.substring(subFrom, s.offsetByCodePoints(subFrom, width))
  }

  def set(col: Int, row: Int, s: String, vertical: Boolean): Unit = {
    val sLength = ExtendedUnicodeHelper.length(s)
    if (col < data.width && (col >= 0 || -col < sLength)) {
      // Make sure the string isn't longer than it needs to be, in particular to
      // avoid sending too much data to our clients.
      val (x, y, truncated) =
      if (vertical) {
        if (row < 0) (col, 0, truncate(s, sLength, -row, data.height))
        else (col, row, truncate(s, sLength, 0, data.height - row))
      }
      else {
        if (col < 0) (0, row, truncate(s, sLength, -col, data.width))
        else (col, row, truncate(s, sLength, 0, data.width - col))
      }
      if (data.set(x, y, truncated, vertical))
        onBufferSet(x, row, truncated, vertical)
    }
  }

  def get(col: Int, row: Int): Char = data.get(col, row).toChar

  def getCodePoint(col: Int, row: Int): Int = data.get(col, row)

  override def getForegroundColor(column: Int, row: Int): Int =
    if (isForegroundFromPalette(column, row)) {
      PackedColor.extractForeground(color(column, row))
    }
    else {
      PackedColor.unpackForeground(color(column, row), data.format)
    }

  override def isForegroundFromPalette(column: Int, row: Int): Boolean =
    data.format.isFromPalette(PackedColor.extractForeground(color(column, row)))

  override def getBackgroundColor(column: Int, row: Int): Int =
    if (isBackgroundFromPalette(column, row)) {
      PackedColor.extractBackground(color(column, row))
    }
    else {
      PackedColor.unpackBackground(color(column, row), data.format)
    }

  override def isBackgroundFromPalette(column: Int, row: Int): Boolean =
    data.format.isFromPalette(PackedColor.extractBackground(color(column, row)))

  override def rawSetText(col: Int, row: Int, text: Array[Array[Char]]): Unit = {
    for (y <- row until ((row + text.length) min data.height)) {
      val line = text(y - row)
      Array.copy(line, 0, data.buffer(y), col, line.length min data.width)
    }
  }

  override def rawSetText(col: Int, row: Int, text: Array[Array[Int]]): Unit = {
    for (y <- row until ((row + text.length) min data.height)) {
      val line = text(y - row)
      Array.copy(line, 0, data.buffer(y), col, line.length min data.width)
    }
  }

  override def rawSetForeground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
    for (y <- row until ((row + color.length) min data.height)) {
      val line = color(y - row)
      for (x <- col until ((col + line.length) min data.width)) {
        val packedBackground = data.color(y)(x) & 0x00FF
        val packedForeground = (data.format.deflate(PackedColor.Color(line(x - col))) << PackedColor.ForegroundShift) & 0xFF00
        data.color(y)(x) = (packedForeground | packedBackground).toShort
      }
    }
  }

  override def rawSetBackground(col: Int, row: Int, color: Array[Array[Int]]): Unit = {
    for (y <- row until ((row + color.length) min data.height)) {
      val line = color(y - row)
      for (x <- col until ((col + line.length) min data.width)) {
        val packedBackground = data.format.deflate(PackedColor.Color(line(x - col))) & 0x00FF
        val packedForeground = data.color(y)(x) & 0xFF00
        data.color(y)(x) = (packedForeground | packedBackground).toShort
      }
    }
  }

  private def color(column: Int, row: Int): Short = {
    if (column < 0 || column >= getWidth || row < 0 || row >= getHeight)
      throw new IndexOutOfBoundsException()
    else data.color(row)(column)
  }
}

