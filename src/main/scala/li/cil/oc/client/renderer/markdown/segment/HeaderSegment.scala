package li.cil.oc.client.renderer.markdown.segment

import net.minecraft.util.EnumChatFormatting

private[markdown] class HeaderSegment(parent: Segment, text: String, val level: Int) extends TextSegment(parent, text) {
  private val fontScale = math.max(2, 5 - level) / 2f

  override protected def scale = Some(fontScale)

  override protected def format = EnumChatFormatting.UNDERLINE.toString

  override def toString: String = s"{HeaderSegment: text = $text, level = $level}"
}
