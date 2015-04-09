package li.cil.oc.client.renderer.markdown.segment

import net.minecraft.util.EnumChatFormatting

private[markdown] class ItalicSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
  override protected def format = EnumChatFormatting.ITALIC.toString

  override def toString: String = s"{ItalicSegment: text = $text}"
}
