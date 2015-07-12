package li.cil.oc.util

import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object Color {
  val Black = 0x444444
  // 0x1E1B1B
  val Red = 0xB3312C
  val Green = 0x339911
  // 0x3B511A
  val Brown = 0x51301A
  val Blue = 0x6666FF
  // 0x253192
  val Purple = 0x7B2FBE
  val Cyan = 0x66FFFF
  // 0x287697
  val LightGray = 0xABABAB
  val Gray = 0x666666
  // 0x434343
  val Pink = 0xD88198
  val Lime = 0x66FF66
  // 0x41CD34
  val Yellow = 0xFFFF66
  // 0xDECF2A
  val LightBlue = 0xAAAAFF
  // 0x6689D3
  val Magenta = 0xC354CD
  val Orange = 0xEB8844
  val White = 0xF0F0F0

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
    "dyeBlack" -> Black,
    "dyeRed" -> Red,
    "dyeGreen" -> Green,
    "dyeBrown" -> Brown,
    "dyeBlue" -> Blue,
    "dyePurple" -> Purple,
    "dyeCyan" -> Cyan,
    "dyeLightGray" -> LightGray,
    "dyeGray" -> Gray,
    "dyePink" -> Pink,
    "dyeLime" -> Lime,
    "dyeYellow" -> Yellow,
    "dyeLightBlue" -> LightBlue,
    "dyeMagenta" -> Magenta,
    "dyeOrange" -> Orange,
    "dyeWhite" -> White)

  val byTier = Array(LightGray, Yellow, Cyan, Magenta)

  def byMeta(meta: Int) = byOreName(dyes(15 - meta))

  def findDye(stack: ItemStack) = byOreName.keys.find(OreDictionary.getOres(_).exists(oreStack => OreDictionary.itemMatches(stack, oreStack, false)))

  def isDye(stack: ItemStack) = findDye(stack).isDefined

  def dyeColor(stack: ItemStack) = findDye(stack).fold(0xFF00FF)(byOreName(_))
}
