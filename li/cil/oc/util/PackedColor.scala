package li.cil.oc.util

object PackedColor {

  object Depth extends Enumeration {
    val OneBit, EightBit, SixteenBit = Value
  }

  private val rMask32 = 0xFF0000
  private val gMask32 = 0x00FF00
  private val bMask32 = 0x0000FF
  private val rShift32 = 16
  private val gShift32 = 8
  private val bShift32 = 0

  // 7 6 5 4 3 2 1 0
  // r r r g g g b b : 3.3.2
  private val rMask8 = Integer.parseInt("11100000", 2)
  private val gMask8 = Integer.parseInt("00011100", 2)
  private val bMask8 = Integer.parseInt("00000011", 2)
  private val rShift8 = 5
  private val gShift8 = 2
  private val bShift8 = 0
  private val rScale8 = 255.0 / 0x7
  private val gScale8 = 255.0 / 0x7
  private val bScale8 = 255.0 / 0x3

  // 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
  //  r  r  r  r  r  g  g  g  g  g  g  b  b  b  b  b : 5.6.5
  private val rMask16 = Integer.parseInt("1111100000000000", 2)
  private val gMask16 = Integer.parseInt("0000011111100000", 2)
  private val bMask16 = Integer.parseInt("0000000000011111", 2)
  private val rShift16 = 11
  private val gShift16 = 5
  private val bShift16 = 0
  private val rScale16 = 255.0 / 0x1F
  private val gScale16 = 255.0 / 0x3F
  private val bScale16 = 255.0 / 0x1F

  private def extractFrom1Bit(c: Short) = if (c == 0) 0x000000 else 0xFFFFFF

  private def compressTo1Bit(c: Int) = (if (c == 0) 0 else 1).toShort

  private def extractFrom8Bit(c: Short) = {
    val r = ((((c & rMask8) >>> rShift8) * rScale8).toInt << rShift32) & rMask32
    val g = ((((c & gMask8) >>> gShift8) * gScale8).toInt << gShift32) & gMask32
    val b = ((((c & bMask8) >>> bShift8) * bScale8).toInt << bShift32) & bMask32
    r | g | b
  }

  private def compressTo8Bit(c: Int) = {
    val r = ((((c & rMask32) >>> rShift32) / rScale8).toInt << rShift8) & rMask8
    val g = ((((c & gMask32) >>> gShift32) / gScale8).toInt << gShift8) & gMask8
    val b = ((((c & bMask32) >>> bShift32) / bScale8).toInt << bShift8) & bMask8
    (r | g | b).toShort
  }

  private def extractFrom16Bit(c: Short) = {
    val r = ((((c & rMask16) >>> rShift16) * rScale16).toInt << rShift32) & rMask32
    val g = ((((c & gMask16) >>> gShift16) * gScale16).toInt << gShift32) & gMask32
    val b = ((((c & bMask16) >>> bShift16) * bScale16).toInt << bShift32) & bMask32
    r | g | b
  }

  private def compressTo16Bit(c: Int) = {
    val r = ((((c & rMask32) >>> rShift32) / rScale16).toInt << rShift16) & rMask16
    val g = ((((c & gMask32) >>> gShift32) / gScale16).toInt << gShift16) & gMask16
    val b = ((((c & bMask32) >>> bShift32) / bScale16).toInt << bShift16) & bMask16
    (r | g | b).toShort
  }

  // Colors are packed: 0xFFFFBBBB (F = foreground, B = background)
  private val fgShift = 16
  private val bgShift = 0

  def pack(foreground: Int, background: Int, depth: Depth.Value) =
    depth match {
      case Depth.OneBit => (compressTo1Bit(foreground) << fgShift) | (compressTo1Bit(background) << bgShift)
      case Depth.EightBit => (compressTo8Bit(foreground) << fgShift) | (compressTo8Bit(background) << bgShift)
      case Depth.SixteenBit => (compressTo16Bit(foreground) << fgShift) | (compressTo16Bit(background) << bgShift)
    }

  def unpackForeground(color: Int, depth: Depth.Value) = {
    val c = (color >>> fgShift).toShort
    depth match {
      case Depth.OneBit => extractFrom1Bit(c)
      case Depth.EightBit => extractFrom8Bit(c)
      case Depth.SixteenBit => extractFrom16Bit(c)
    }
  }

  def unpackBackground(color: Int, depth: Depth.Value) = {
    val c = (color >>> bgShift).toShort
    depth match {
      case Depth.OneBit => extractFrom1Bit(c)
      case Depth.EightBit => extractFrom8Bit(c)
      case Depth.SixteenBit => extractFrom16Bit(c)
    }
  }
}
