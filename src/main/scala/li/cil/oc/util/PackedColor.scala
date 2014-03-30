package li.cil.oc.util

object PackedColor {

  object Depth extends Enumeration {
    val OneBit, FourBit, EightBit = Value

    def bits(depth: Depth.Value) = depth match {
      case OneBit => 1
      case FourBit => 4
      case EightBit => 8
    }
  }

  private val rMask32 = 0xFF0000
  private val gMask32 = 0x00FF00
  private val bMask32 = 0x0000FF
  private val rShift32 = 16
  private val gShift32 = 8
  private val bShift32 = 0

  private abstract class ColorFormat {
    def inflate(value: Int): Int

    def deflate(value: Int): Int
  }

  private class SingleBitFormat extends ColorFormat {
    def inflate(value: Int) = if (value == 0) 0x000000 else 0xFFFFFF

    def deflate(value: Int) = if (value == 0) 0 else 1
  }

  private class MultiBitFormat(rBits: Int, gBits: Int, bBits: Int) extends ColorFormat {
    def mask(nBits: Int) = 0xFFFFFFFF >>> (32 - nBits)

    private val bShift = 0
    private val gShift = bBits
    private val rShift = gShift + gBits

    private val bMask = mask(bBits) << bShift
    private val gMask = mask(gBits) << gShift
    private val rMask = mask(rBits) << rShift

    private val bScale = 255.0 / ((1 << bBits) - 1)
    private val gScale = 255.0 / ((1 << gBits) - 1)
    private val rScale = 255.0 / ((1 << rBits) - 1)

    def inflate(value: Int) = {
      val r = ((((value & rMask) >>> rShift) * rScale + 0.5).toInt << rShift32) & rMask32
      val g = ((((value & gMask) >>> gShift) * gScale + 0.5).toInt << gShift32) & gMask32
      val b = ((((value & bMask) >>> bShift) * bScale + 0.5).toInt << bShift32) & bMask32
      r | g | b
    }

    def deflate(value: Int) = {
      val r = ((((value & rMask32) >>> rShift32) / rScale + 0.5).toInt << rShift) & rMask
      val g = ((((value & gMask32) >>> gShift32) / gScale + 0.5).toInt << gShift) & gMask
      val b = ((((value & bMask32) >>> bShift32) / bScale + 0.5).toInt << bShift) & bMask
      r | g | b
    }
  }

  private val formats = Map(
    Depth.OneBit -> new SingleBitFormat(),
    Depth.FourBit -> new MultiBitFormat(1, 2, 1),
    Depth.EightBit -> new MultiBitFormat(3, 3, 2))

  // Colors are packed: 0xFFBB (F = foreground, B = background)
  private val fgShift = 8
  private val bgMask = 0x000000FF

  def pack(foreground: Int, background: Int, depth: Depth.Value) = {
    val format = formats(depth)
    ((format.deflate(foreground) << fgShift) | format.deflate(background)).toShort
  }

  def unpackForeground(color: Short, depth: Depth.Value) =
    formats(depth).inflate((color & 0xFFFF) >>> fgShift)

  def unpackBackground(color: Short, depth: Depth.Value) =
    formats(depth).inflate(color & bgMask)
}
