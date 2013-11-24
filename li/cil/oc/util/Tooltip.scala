package li.cil.oc.util

import net.minecraft.util.EnumChatFormatting

object Tooltip {

  val Reset = "\u00A7" + EnumChatFormatting.RESET.func_96298_a

  object Color {
    val Aqua = "\u00A7" + EnumChatFormatting.AQUA.func_96298_a
    val Black = "\u00A7" + EnumChatFormatting.BLACK.func_96298_a
    val Blue = "\u00A7" + EnumChatFormatting.BLUE.func_96298_a
    val DarkAqua = "\u00A7" + EnumChatFormatting.DARK_AQUA.func_96298_a
    val DarkBlue = "\u00A7" + EnumChatFormatting.DARK_BLUE.func_96298_a
    val DarkGray = "\u00A7" + EnumChatFormatting.DARK_GRAY.func_96298_a
    val DarkGreen = "\u00A7" + EnumChatFormatting.DARK_GREEN.func_96298_a
    val DarkPurple = "\u00A7" + EnumChatFormatting.DARK_PURPLE.func_96298_a
    val DarkRed = "\u00A7" + EnumChatFormatting.DARK_RED.func_96298_a
    val Gold = "\u00A7" + EnumChatFormatting.GOLD.func_96298_a
    val Gray = "\u00A7" + EnumChatFormatting.GRAY.func_96298_a
    val Green = "\u00A7" + EnumChatFormatting.GREEN.func_96298_a
    val LightPurple = "\u00A7" + EnumChatFormatting.LIGHT_PURPLE.func_96298_a
    val Red = "\u00A7" + EnumChatFormatting.RED.func_96298_a
    val White = "\u00A7" + EnumChatFormatting.WHITE.func_96298_a
    val Yellow = "\u00A7" + EnumChatFormatting.YELLOW.func_96298_a
  }

  object Format {
    val Obfuscated = "\u00A7" + EnumChatFormatting.OBFUSCATED.func_96298_a
    val Bold = "\u00A7" + EnumChatFormatting.BOLD.func_96298_a
    val StrikeThrough = "\u00A7" + EnumChatFormatting.STRIKETHROUGH.func_96298_a
    val Underline = "\u00A7" + EnumChatFormatting.UNDERLINE.func_96298_a
    val Italic = "\u00A7" + EnumChatFormatting.ITALIC.func_96298_a
  }

  def format(value: String, format: String) = format + value + Reset + Color.Gray
}
