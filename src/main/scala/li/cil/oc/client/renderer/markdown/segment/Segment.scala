package li.cil.oc.client.renderer.markdown.segment

import net.minecraft.client.gui.FontRenderer

import scala.annotation.tailrec
import scala.util.matching.Regex

trait Segment {
  /**
   * Computes the height of this segment, in pixels, given it starts at the
   * specified indent into the current line, with the specified maximum
   * allowed width.
   */
  def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = 0

  /**
   * Computes the width of the last line of this segment, given it starts
   * at the specified indent into the current line, with the specified
   * maximum allowed width.
   * If the segment remains on the same line, returns the new end of the
   * line (i.e. indent plus width of the segment).
   */
  def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = 0

  def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = None

  // Used when rendering, to compute the style of a nested segment.
  protected def parent: Segment

  @tailrec protected final def root: Segment = if (parent == null) this else parent.root

  // Used during construction, checks a segment for inner segments.
  private[markdown] def refine(pattern: Regex, factory: (Segment, Regex.Match) => Segment): Iterable[Segment] = Iterable(this)

  // Set after construction of document, used for formatting (e.g. newline height).
  private[markdown] var previous: Segment = null
}
