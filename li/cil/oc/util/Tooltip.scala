package li.cil.oc.util

import net.minecraft.util.EnumChatFormatting

object Tooltip {

  val Reset = "\u00A7" + EnumChatFormatting.RESET.func_96298_a // 'r'

  object Color {
    val Aqua = "\u00A7" + EnumChatFormatting.AQUA.func_96298_a // 'b'
    val Black = "\u00A7" + EnumChatFormatting.BLACK.func_96298_a // '0'
    val Blue = "\u00A7" + EnumChatFormatting.BLUE.func_96298_a // '9'
    val DarkAqua = "\u00A7" + EnumChatFormatting.DARK_AQUA.func_96298_a // '3'
    val DarkBlue = "\u00A7" + EnumChatFormatting.DARK_BLUE.func_96298_a // '1'
    val DarkGray = "\u00A7" + EnumChatFormatting.DARK_GRAY.func_96298_a // '8'
    val DarkGreen = "\u00A7" + EnumChatFormatting.DARK_GREEN.func_96298_a // '2'
    val DarkPurple = "\u00A7" + EnumChatFormatting.DARK_PURPLE.func_96298_a // '5'
    val DarkRed = "\u00A7" + EnumChatFormatting.DARK_RED.func_96298_a // '4'
    val Gold = "\u00A7" + EnumChatFormatting.GOLD.func_96298_a // '6'
    val Gray = "\u00A7" + EnumChatFormatting.GRAY.func_96298_a // '7'
    val Green = "\u00A7" + EnumChatFormatting.GREEN.func_96298_a // 'a'
    val LightPurple = "\u00A7" + EnumChatFormatting.LIGHT_PURPLE.func_96298_a // 'd'
    val Red = "\u00A7" + EnumChatFormatting.RED.func_96298_a // 'c'
    val White = "\u00A7" + EnumChatFormatting.WHITE.func_96298_a // 'f'
    val Yellow = "\u00A7" + EnumChatFormatting.YELLOW.func_96298_a // 'e'
  }

  object Format {
    val Obfuscated = "\u00A7" + EnumChatFormatting.OBFUSCATED.func_96298_a // 'k'
    val Bold = "\u00A7" + EnumChatFormatting.BOLD.func_96298_a // 'l'
    val StrikeThrough = "\u00A7" + EnumChatFormatting.STRIKETHROUGH.func_96298_a // 'm'
    val Underline = "\u00A7" + EnumChatFormatting.UNDERLINE.func_96298_a // 'n'
    val Italic = "\u00A7" + EnumChatFormatting.ITALIC.func_96298_a // 'o'
  }

  def format(value: String, format: String) = format + value + Reset + Color.Gray
}
