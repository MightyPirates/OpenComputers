package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.Document
import net.minecraft.client.gui.FontRenderer

private[markdown] class NewLineSegment extends Segment {
  override protected def parent: Segment = null

  override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = previous match {
    case segment: TextSegment => (Document.lineHeight(renderer) * segment.resolvedScale).toInt
    case _ => Document.lineHeight(renderer)
  }

  override def toString: String = s"{NewLineSegment}"
}
