package li.cil.oc.util

import li.cil.oc.api.Persistable
import net.minecraft.nbt.NBTTagCompound

object PackedColor {

  object Depth extends Enumeration {
    val OneBit, FourBit, EightBit = Value

    def bits(depth: Depth.Value) = depth match {
      case OneBit => 1
      case FourBit => 4
      case EightBit => 8
    }

    def format(depth: Depth.Value) = depth match {
      case OneBit => SingleBitFormat
      case FourBit => new MutablePaletteFormat
      case EightBit => new HybridFormat
    }
  }

  private val rShift32 = 16
  private val gShift32 = 8
  private val bShift32 = 0

  private def extract(value: Int) = {
    val r = (value >>> rShift32) & 0xFF
    val g = (value >>> gShift32) & 0xFF
    val b = (value >>> bShift32) & 0xFF
    (r, g, b)
  }

  trait ColorFormat extends Persistable {
    def depth: Depth.Value

    def inflate(value: Int): Int

    def deflate(value: Int): Int

    override def load(nbt: NBTTagCompound) {}

    override def save(nbt: NBTTagCompound) {}
  }

  object SingleBitFormat extends ColorFormat {
    override def depth = Depth.OneBit

    override def inflate(value: Int) = if (value == 0) 0x000000 else 0xFFFFFF

    override def deflate(value: Int) = if (value == 0) 0 else 1
  }

  abstract class PaletteFormat extends ColorFormat {
    override def inflate(value: Int) = palette(math.max(0, math.min(palette.length - 1, value)))

    override def deflate(value: Int) = palette.map(delta(value, _)).zipWithIndex.minBy(_._1)._2

    protected def palette: Array[Int]

    protected def delta(colorA: Int, colorB: Int) = {
      val (rA, gA, bA) = extract(colorA)
      val (rB, gB, bB) = extract(colorB)
      val dr = rA - rB
      val dg = gA - gB
      val db = bA - bB
      0.2126 * dr * dr + 0.7152 * dg * dg + 0.0722 * db * db
    }
  }

  class MutablePaletteFormat extends PaletteFormat {
    override def depth = Depth.FourBit

    def apply(index: Int) = palette(index)

    def update(index: Int, value: Int) = palette(index) = value

    protected val palette = Array(
      0x000000, 0xFF3333, 0x336600, 0x663300,
      0x333399, 0x9933CC, 0x336699, 0xCCCCCC,
      0x333333, 0xFF6699, 0x33CC33, 0xFFFF33,
      0x6699FF, 0xCC66CC, 0xFFCC33, 0xFFFFFF)

    override def load(nbt: NBTTagCompound) {
      val loaded = nbt.getIntArray("palette")
      Array.copy(loaded, 0, palette, 0, math.min(loaded.length, palette.length))
    }

    override def save(nbt: NBTTagCompound) {
      nbt.setIntArray("palette", palette)
    }
  }

  class HybridFormat extends MutablePaletteFormat {
    private val reds = 6
    private val greens = 8
    private val blues = 5

    // Initialize palette to grayscale, excluding black and white, because
    // those are already contained in the normal color cube.
    for (i <- 0 until palette.length) {
      val shade = 0xFF * (i + 1) / (palette.length + 1)
      this(i) = (shade << rShift32) | (shade << gShift32) | (shade << bShift32)
    }

    override def depth = Depth.EightBit

    override def inflate(value: Int) = {
      if (value < palette.length) super.inflate(value)
      else {
        val index = value - palette.length
        val idxB = index % blues
        val idxG = (index / blues) % greens
        val idxR = (index / blues / greens) % reds
        val r = (idxR * 0xFF / (reds - 1.0) + 0.5).toInt
        val g = (idxG * 0xFF / (greens - 1.0) + 0.5).toInt
        val b = (idxB * 0xFF / (blues - 1.0) + 0.5).toInt
        (r << rShift32) | (g << gShift32) | (b << bShift32)
      }
    }

    override def deflate(value: Int) = {
      val (r, g, b) = extract(value)
      val idxR = (r * (reds - 1.0) / 0xFF + 0.5).toInt
      val idxG = (g * (greens - 1.0) / 0xFF + 0.5).toInt
      val idxB = (b * (blues - 1.0) / 0xFF + 0.5).toInt
      palette.length + idxR * greens * blues + idxG * blues + idxB
    }
  }

  // Colors are packed: 0xFFBB (F = foreground, B = background)
  private val fgShift = 8
  private val bgMask = 0x000000FF

  def pack(foreground: Int, background: Int, format: ColorFormat) = {
    ((format.deflate(foreground) << fgShift) | format.deflate(background)).toShort
  }

  def unpackForeground(color: Short, format: ColorFormat) =
    format.inflate((color & 0xFFFF) >>> fgShift)

  def unpackBackground(color: Short, format: ColorFormat) =
    format.inflate(color & bgMask)
}
