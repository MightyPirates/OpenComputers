package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.util.EnumChatFormatting

private[markdown] class ItalicSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
  override protected def format = EnumChatFormatting.ITALIC.toString

  override def toString(format: MarkupFormat.Value): String = format match {
    case MarkupFormat.Markdown => s"*$text*"
    case MarkupFormat.IGWMod => s"[prefix{o}]$text [prefix{}]"
  }
}
