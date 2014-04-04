package li.cil.oc.util

import net.minecraft.item.{ItemDye, ItemStack}

object Color {
  def Black = 0x444444 // 0x1E1B1B
  def Red = 0xB3312C
  def Green = 0x339911 // 0x3B511A
  def Brown = 0x51301A
  def Blue = 0x6666FF // 0x253192
  def Purple = 0x7B2FBE
  def Cyan = 0x66FFFF // 0x287697
  def Silver = 0xABABAB
  def Gray = 0x666666 // 0x434343
  def Pink = 0xD88198
  def Lime = 0x66FF66 // 0x41CD34
  def Yellow = 0xFFFF66 // 0xDECF2A
  def LightBlue = 0xAAAAFF // 0x6689D3
  def Magenta = 0xC354CD
  def Orange = 0xEB8844
  def White = 0xF0F0F0

  def byDyeNumber = Array(Black, Red, Green, Brown, Blue, Purple, Cyan, Silver, Gray, Pink, Lime, Yellow, LightBlue, Magenta, Orange, White)

  val byTier = Array(Silver, Yellow, Cyan)

  def isDye(stack: ItemStack) = stack != null && stack.getItem.isInstanceOf[ItemDye]

  def dyeColor(stack: ItemStack) = stack.getItem match {
    case dye: ItemDye => byDyeNumber(math.max(0, math.min(15, stack.getItemDamage)))
    case _ => 0xFF00FF
  }
}
