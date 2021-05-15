package li.cil.oc.util

import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object Color {
  val rgbValues = Map(
    EnumDyeColor.BLACK -> 0x444444, // 0x1E1B1B
    EnumDyeColor.RED -> 0xB3312C,
    EnumDyeColor.GREEN -> 0x339911, // 0x3B511A
    EnumDyeColor.BROWN -> 0x51301A,
    EnumDyeColor.BLUE -> 0x6666FF, // 0x253192
    EnumDyeColor.PURPLE -> 0x7B2FBE,
    EnumDyeColor.CYAN -> 0x66FFFF, // 0x287697
    EnumDyeColor.SILVER -> 0xABABAB,
    EnumDyeColor.GRAY -> 0x666666, // 0x434343
    EnumDyeColor.PINK -> 0xD88198,
    EnumDyeColor.LIME -> 0x66FF66, // 0x41CD34
    EnumDyeColor.YELLOW -> 0xFFFF66, // 0xDECF2A
    EnumDyeColor.LIGHT_BLUE -> 0xAAAAFF, // 0x6689D3
    EnumDyeColor.MAGENTA -> 0xC354CD,
    EnumDyeColor.ORANGE -> 0xEB8844,
    EnumDyeColor.WHITE -> 0xF0F0F0
  )

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

  val byOreName = Map(
    "dyeBlack" -> EnumDyeColor.BLACK,
    "dyeRed" -> EnumDyeColor.RED,
    "dyeGreen" -> EnumDyeColor.GREEN,
    "dyeBrown" -> EnumDyeColor.BROWN,
    "dyeBlue" -> EnumDyeColor.BLUE,
    "dyePurple" -> EnumDyeColor.PURPLE,
    "dyeCyan" -> EnumDyeColor.CYAN,
    "dyeLightGray" -> EnumDyeColor.SILVER,
    "dyeGray" -> EnumDyeColor.GRAY,
    "dyePink" -> EnumDyeColor.PINK,
    "dyeLime" -> EnumDyeColor.LIME,
    "dyeYellow" -> EnumDyeColor.YELLOW,
    "dyeLightBlue" -> EnumDyeColor.LIGHT_BLUE,
    "dyeMagenta" -> EnumDyeColor.MAGENTA,
    "dyeOrange" -> EnumDyeColor.ORANGE,
    "dyeWhite" -> EnumDyeColor.WHITE)

  val byTier = Array(EnumDyeColor.SILVER, EnumDyeColor.YELLOW, EnumDyeColor.CYAN, EnumDyeColor.MAGENTA)

  def byMeta(meta: EnumDyeColor) = byOreName(dyes(meta.getDyeDamage))

  def findDye(stack: ItemStack) = byOreName.keys.find(OreDictionary.getOres(_).exists(oreStack => OreDictionary.itemMatches(stack, oreStack, false)))

  def isDye(stack: ItemStack) = findDye(stack).isDefined

  def dyeColor(stack: ItemStack) = findDye(stack).fold(EnumDyeColor.MAGENTA)(byOreName(_))
}
