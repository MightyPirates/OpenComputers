package li.cil.oc.util

import net.minecraft.item.DyeColor
import net.minecraft.item.ItemStack

import scala.collection.convert.ImplicitConversionsToScala._

object Color {
  val rgbValues = Map(
    DyeColor.BLACK -> 0x444444, // 0x1E1B1B
    DyeColor.RED -> 0xB3312C,
    DyeColor.GREEN -> 0x339911, // 0x3B511A
    DyeColor.BROWN -> 0x51301A,
    DyeColor.BLUE -> 0x6666FF, // 0x253192
    DyeColor.PURPLE -> 0x7B2FBE,
    DyeColor.CYAN -> 0x66FFFF, // 0x287697
    DyeColor.LIGHT_GRAY -> 0xABABAB,
    DyeColor.GRAY -> 0x666666, // 0x434343
    DyeColor.PINK -> 0xD88198,
    DyeColor.LIME -> 0x66FF66, // 0x41CD34
    DyeColor.YELLOW -> 0xFFFF66, // 0xDECF2A
    DyeColor.LIGHT_BLUE -> 0xAAAAFF, // 0x6689D3
    DyeColor.MAGENTA -> 0xC354CD,
    DyeColor.ORANGE -> 0xEB8844,
    DyeColor.WHITE -> 0xF0F0F0
  )

  @Deprecated
  val dyes = Array(
    "dyeBlack",
    "dyeRed",
    "dyeGreen",
    "dyeBrown",
    "dyeBlue",
    "dyePurple",
    "dyeCyan",
    "dyeLightGray",
    "dyeGray",
    "dyePink",
    "dyeLime",
    "dyeYellow",
    "dyeLightBlue",
    "dyeMagenta",
    "dyeOrange",
    "dyeWhite")

  @Deprecated
  val byOreName = Map(
    "dyeBlack" -> DyeColor.BLACK,
    "dyeRed" -> DyeColor.RED,
    "dyeGreen" -> DyeColor.GREEN,
    "dyeBrown" -> DyeColor.BROWN,
    "dyeBlue" -> DyeColor.BLUE,
    "dyePurple" -> DyeColor.PURPLE,
    "dyeCyan" -> DyeColor.CYAN,
    "dyeLightGray" -> DyeColor.LIGHT_GRAY,
    "dyeGray" -> DyeColor.GRAY,
    "dyePink" -> DyeColor.PINK,
    "dyeLime" -> DyeColor.LIME,
    "dyeYellow" -> DyeColor.YELLOW,
    "dyeLightBlue" -> DyeColor.LIGHT_BLUE,
    "dyeMagenta" -> DyeColor.MAGENTA,
    "dyeOrange" -> DyeColor.ORANGE,
    "dyeWhite" -> DyeColor.WHITE)

  @Deprecated
  val byId = dyes.map(name => (byOreName(name).getId, name)).toMap

  private val byTag = DyeColor.values.map(col => (col.getTag.getName, col)).toMap

  val byTier = Array(DyeColor.LIGHT_GRAY, DyeColor.YELLOW, DyeColor.CYAN, DyeColor.MAGENTA)

  def findDye(stack: ItemStack) = byTag.keys.find(stack.getItem.getTags.contains)

  def isDye(stack: ItemStack) = findDye(stack).isDefined

  def dyeColor(stack: ItemStack) = findDye(stack).fold(DyeColor.MAGENTA)(byTag(_))
}
