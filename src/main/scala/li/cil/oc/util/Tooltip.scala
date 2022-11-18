package li.cil.oc.util

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.text.CharacterManager.ISliceAcceptor
import net.minecraft.util.text.Style
import net.minecraft.util.text.TextFormatting

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

object Tooltip {
  private val maxWidth = 220

  private def font = Minecraft.getInstance.font

  val DefaultStyle = Style.EMPTY.applyFormat(TextFormatting.GRAY)

  def get(name: String, args: Any*): java.util.List[String] = {
    if (!Localization.canLocalize(Settings.namespace + "tooltip." + name)) return Seq.empty[String]
    val tooltip = Localization.localizeImmediately("tooltip." + name).
      format(args.map(_.toString): _*)
    if (font == null) return tooltip.lines.toList // Some mods request tooltips before font renderer is available.
    val isSubTooltip = name.contains(".")
    val shouldShorten = (isSubTooltip || font.width(tooltip) > maxWidth) && !KeyBindings.showExtendedTooltips
    if (shouldShorten) {
      if (isSubTooltip) Seq.empty[String]
      else Seq(Localization.localizeImmediately("tooltip.toolong", KeyBindings.getKeyBindingName(KeyBindings.extendedTooltip)))
    }
    else tooltip.
      lines.
      map(wrap(font, _, maxWidth).map(_.asInstanceOf[String].trim() + " ")).
      flatten.
      toList
  }

  def extended(name: String, args: Any*): java.util.List[String] =
    if (KeyBindings.showExtendedTooltips) {
      Localization.localizeImmediately("tooltip." + name).
        format(args.map(_.toString): _*).
        lines.
        map(wrap(font, _, maxWidth).map(_.asInstanceOf[String].trim() + " ")).
        flatten.
        toList
    }
    else Seq.empty[String]

  private def wrap(font: FontRenderer, line: String, width: Int): java.util.List[String] = {
    val list = new java.util.ArrayList[String]
    font.getSplitter.splitLines(line, width, net.minecraft.util.text.Style.EMPTY, true, new ISliceAcceptor {
      override def accept(style: Style, start: Int, end: Int) = list.add(line.substring(start, end))
    })
    list
  }
}
