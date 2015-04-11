package li.cil.oc.client.renderer.markdown.segment

import net.minecraft.util.EnumChatFormatting

private[markdown] class StrikethroughSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
  override protected def format = EnumChatFormatting.STRIKETHROUGH.toString

  override def toString: String = s"{StrikethroughSegment: text = $text}"
}
