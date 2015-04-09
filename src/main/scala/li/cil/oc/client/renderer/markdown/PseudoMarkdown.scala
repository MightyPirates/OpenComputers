package li.cil.oc.client.renderer.markdown

import li.cil.oc.client.Manual
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment
import li.cil.oc.client.renderer.markdown.segment.Segment
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

import scala.util.matching.Regex

/**
 * Primitive Markdown parser, only supports a very small subset. Used for
 * parsing documentation into segments, to be displayed in a GUI somewhere.
 */
object PseudoMarkdown {
  /**
   * Parses a plain text document into a list of segments.
   */
  def parse(document: Iterable[String]): Iterable[Segment] = {
    var segments = document.flatMap(line => Iterable(new segment.TextSegment(null, Option(line).getOrElse("")), new segment.NewLineSegment())).toArray
    for ((pattern, factory) <- segmentTypes) {
      segments = segments.flatMap(_.refine(pattern, factory))
    }
    for (Array(s1, s2) <- segments.sliding(2)) {
      s2.previous = s1
    }
    segments
  }

  /**
   * Renders a list of segments and tooltips if a segment with a tooltip is hovered.
   * Returns a link address if a link is hovered.
   */
  def render(document: Iterable[Segment], x: Int, y: Int, maxWidth: Int, maxHeight: Int, yOffset: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    // Create a flat area in the depth buffer.
    GL11.glPushMatrix()
    GL11.glTranslatef(0, 0, 1)
    GL11.glDepthMask(true)
    GL11.glColor4f(0.01f, 0.01f, 0.01f, 1)
    GL11.glBegin(GL11.GL_QUADS)
    GL11.glVertex2f(x - 1, y - 1)
    GL11.glVertex2f(x - 1, y + 1 + maxHeight)
    GL11.glVertex2f(x + 1 + maxWidth, y + 1 + maxHeight)
    GL11.glVertex2f(x + 1 + maxWidth, y - 1)
    GL11.glEnd()

    // Use that flat area to mask the output area.
    GL11.glDepthMask(false)
    GL11.glDepthFunc(GL11.GL_EQUAL)

    // Actual rendering.
    var hovered: Option[InteractiveSegment] = None
    var currentX = 0
    var currentY = 0
    for (segment <- document) {
      val result = segment.render(x, y + currentY - yOffset, currentX, maxWidth, y, maxHeight - (currentY - yOffset), renderer, mouseX, mouseY)
      hovered = hovered.orElse(result)
      currentY += segment.height(currentX, maxWidth, renderer)
      currentX = segment.width(currentX, maxWidth, renderer)
    }
    if (mouseX < x || mouseX > x + maxWidth || mouseY < y || mouseY > y + maxHeight) hovered = None
    hovered.foreach(_.notifyHover())

    // Restore all the things.
    GL11.glDepthFunc(GL11.GL_LEQUAL)
    GL11.glPopMatrix()

    hovered
  }

  /**
   * Compute the overall height of a document, for computation of scroll offsets.
   */
  def height(document: Iterable[Segment], maxWidth: Int, renderer: FontRenderer): Int = {
    var currentX = 0
    var currentY = 0
    for (segment <- document) {
      currentY += segment.height(currentX, maxWidth, renderer)
      currentX = segment.width(currentX, maxWidth, renderer)
    }
    currentY
  }

  /**
   * Line height for a normal line of text.
   */
  def lineHeight(renderer: FontRenderer): Int = renderer.FONT_HEIGHT + 1

  // ----------------------------------------------------------------------- //

  private def HeaderSegment(s: Segment, m: Regex.Match) = new segment.HeaderSegment(s, m.group(2), m.group(1).length)

  private def LinkSegment(s: Segment, m: Regex.Match) = new segment.LinkSegment(s, m.group(1), m.group(2))

  private def BoldSegment(s: Segment, m: Regex.Match) = new segment.BoldSegment(s, m.group(2))

  private def ItalicSegment(s: Segment, m: Regex.Match) = new segment.ItalicSegment(s, m.group(2))

  private def StrikethroughSegment(s: Segment, m: Regex.Match) = new segment.StrikethroughSegment(s, m.group(1))

  private def ImageSegment(s: Segment, m: Regex.Match) = {
    try Option(Manual.imageFor(m.group(2))) match {
      case Some(renderer) => new segment.RenderSegment(s, m.group(1), renderer)
      case _ => new segment.ImageSegment(s, m.group(1), m.group(2))
    } catch {
      case t: Throwable => new segment.TextSegment(s, Option(t.toString).getOrElse("Unknown error."))
    }
  }

  // ----------------------------------------------------------------------- //

  private val segmentTypes = Array(
    """^(#+)\s(.*)""".r -> HeaderSegment _, // headers: # ...
    """!\[([^\[]*)\]\(([^\)]+)\)""".r -> ImageSegment _, // images: ![...](...)
    """\[([^\[]+)\]\(([^\)]+)\)""".r -> LinkSegment _, // links: [...](...)
    """(\*\*|__)(\S.*?\S|$)\1""".r -> BoldSegment _, // bold: **...** | __...__
    """(\*|_)(\S.*?\S|$)\1""".r -> ItalicSegment _, // italic: *...* | _..._
    """~~(\S.*?\S|$)~~""".r -> StrikethroughSegment _ // strikethrough: ~~...~~
  )
}
