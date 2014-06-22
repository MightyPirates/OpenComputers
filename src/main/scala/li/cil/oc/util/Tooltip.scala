package li.cil.oc.util

import li.cil.oc.Localization
import li.cil.oc.client.KeyBindings
import net.minecraft.client.Minecraft
import org.lwjgl.input.Keyboard

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object Tooltip {
  val maxWidth = 220

  def get(name: String, args: Any*): java.util.List[String] = {
    val tooltip = Localization.localizeImmediately("tooltip." + name).format(args.map(_.toString): _*)
    val isSubTooltip = name.contains(".")
    val font = Minecraft.getMinecraft.fontRenderer
    val shouldShorten = (isSubTooltip || font.getStringWidth(tooltip) > maxWidth) && !KeyBindings.showExtendedTooltips
    if (shouldShorten) {
      if (isSubTooltip) Seq.empty[String]
      else Seq(Localization.localizeImmediately("tooltip.TooLong", Keyboard.getKeyName(KeyBindings.extendedTooltip.keyCode)))
    }
    else {
      val nl = """\[nl\]"""
      tooltip.
        split(nl).
        map(font.listFormattedStringToWidth(_, maxWidth).map(_.asInstanceOf[String].trim() + " ")).
        flatten.
        toList
    }
  }
}
