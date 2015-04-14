package li.cil.oc.client.renderer.markdown.segment

import net.minecraft.util.EnumChatFormatting

private[markdown] class BoldSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
  override protected def format = EnumChatFormatting.BOLD.toString

  override def toString: String = s"{BoldSegment: text = $text}"
}
