package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.util.text.TextFormatting

private[markdown] class HeaderSegment(parent: Segment, text: String, val level: Int) extends TextSegment(parent, text) {
  private val fontScale = math.max(2, 5 - level) / 2f

  override protected def scale = Some(fontScale)

  override protected def format = TextFormatting.UNDERLINE.toString

  override def toString(format: MarkupFormat.Value): String = format match {
    case MarkupFormat.Markdown => s"${"#" * level} $text"
    case MarkupFormat.IGWMod => s"[prefix{l}]$text [prefix{}]"
  }
}
