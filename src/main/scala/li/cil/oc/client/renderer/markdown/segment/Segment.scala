package li.cil.oc.client.renderer.markdown.segment

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.client.gui.FontRenderer

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.matching.Regex

trait Segment {
  /**
   * Parent segment, i.e. the segment this segment was refined from.
   * Each line starts as a TextSegment that is refined based into segments
   * based on the handled formatting rules / patterns.
   */
  def parent: Segment

  /**
   * The root segment, i.e. the original parent of this segment.
   */
  @tailrec final def root: Segment = if (parent == null) this else parent.root

  /**
   * Get the X coordinate at which to render the next segment.
   *
   * For flowing/inline segments this will be to the right of the last line
   * this segment renders, for block segments it will be at the start of
   * the next line below this segment.
   *
   * The coordinates in this context are relative to (0,0).
   */
  def nextX(indent: Int, maxWidth: Int, renderer: FontRenderer): Int

  /**
   * Get the Y coordinate at which to render the next segment.
   *
   * For flowing/inline segments this will be the same level as the last line
   * this segment renders, unless it's the last segment on its line. For block
   * segments and last-on-line segments this will be the next line after.
   *
   * The coordinates in this context are relative to (0,0).
   */
  def nextY(indent: Int, maxWidth: Int, renderer: FontRenderer): Int

  /**
   * Render the segment at the specified coordinates with the specified
   * properties.
   */
  def render(stack: MatrixStack, x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = None

  def renderAsText(format: MarkupFormat.Value): Iterable[String] = {
    var segment = this
    val result = mutable.Buffer.empty[String]
    val builder = mutable.StringBuilder.newBuilder
    while (segment != null) {
      builder.append(segment.toString(format))
      if (segment.isLast) {
        result += builder.toString()
        builder.clear()
      }
      segment = segment.next
    }
    result.toIterable
  }

  def toString(format: MarkupFormat.Value): String

  override def toString: String = toString(MarkupFormat.Markdown)

  // ----------------------------------------------------------------------- //

  // Used during construction, checks a segment for inner segments.
  private[markdown] def refine(pattern: Regex, factory: (Segment, Regex.Match) => Segment): Iterable[Segment] = Iterable(this)

  // Set after construction of document, used for formatting, specifically
  // to compute the height for last segment on a line (to force a new line).
  private[markdown] var next: Segment = null

  // Utility method to check if the segment is the last on a line.
  private[markdown] def isLast = next == null || root != next.root
}
