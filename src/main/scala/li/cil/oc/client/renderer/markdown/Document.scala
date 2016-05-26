package li.cil.oc.client.renderer.markdown

import li.cil.oc.api
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment
import li.cil.oc.client.renderer.markdown.segment.Segment
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

import scala.collection.Iterable
import scala.util.matching.Regex

/**
 * Primitive Markdown parser, only supports a very small subset. Used for
 * parsing documentation into segments, to be displayed in a GUI somewhere.
 *
 * General usage is: parse a string using parse(), render it using render().
 *
 * The parser generates a list of segments, each segment representing a part
 * of the document, with a specific formatting / render type. For example,
 * links are their own segments, a bold section in a link would be its own
 * section and so on.
 * The data structure is essentially a very flat multi-tree, where the segments
 * returned are the leaves, and the roots are the individual lines, represented
 * as text segments.
 * Formatting is done by accumulating formatting information over the parent
 * nodes, up to the root.
 */
object Document {
  /**
   * Parses a plain text document into a list of segments.
   */
  def parse(document: Iterable[String]): Segment = {
    var segments: Iterable[Segment] = document.map(line => new segment.TextSegment(null, Option(line).fold("")(_.reverse.dropWhile(_.isWhitespace).reverse)))
    for ((pattern, factory) <- segmentTypes) {
      segments = segments.flatMap(_.refine(pattern, factory))
    }
    for (window <- segments.sliding(2) if window.size == 2) {
      window.head.next = window.last
    }
    segments.head
  }

  /**
   * Compute the overall height of a document, e.g. for computation of scroll offsets.
   */
  def height(document: Segment, maxWidth: Int, renderer: FontRenderer): Int = {
    var currentX = 0
    var currentY = 0
    var segment = document
    while (segment != null) {
      currentY += segment.nextY(currentX, maxWidth, renderer)
      currentX = segment.nextX(currentX, maxWidth, renderer)
      segment = segment.next
    }
    currentY
  }

  /**
   * Line height for a normal line of text.
   */
  def lineHeight(renderer: FontRenderer): Int = renderer.FONT_HEIGHT + 1

  /**
   * Renders a list of segments and tooltips if a segment with a tooltip is hovered.
   * Returns the hovered interactive segment, if any.
   */
  def render(document: Segment, x: Int, y: Int, maxWidth: Int, maxHeight: Int, yOffset: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val mc = Minecraft.getMinecraft

    RenderState.pushAttrib()

    // On some systems/drivers/graphics cards the next calls won't update the
    // depth buffer correctly if alpha test is enabled. Guess how we found out?
    // By noticing that on those systems it only worked while chat messages
    // were visible. Yeah. I know.
    GlStateManager.disableAlpha()

    // Clear depth mask, then create masks in foreground above and below scroll area.
    GlStateManager.color(1, 1, 1, 1)
    GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT)
    GlStateManager.enableDepth()
    GlStateManager.depthFunc(GL11.GL_LEQUAL)
    GlStateManager.depthMask(true)
    GlStateManager.colorMask(false, false, false, false)

    GlStateManager.pushMatrix()
    GL11.glTranslatef(0, 0, 500)
    GL11.glBegin(GL11.GL_QUADS)
    GL11.glVertex2f(0, y)
    GL11.glVertex2f(mc.displayWidth, y)
    GL11.glVertex2f(mc.displayWidth, 0)
    GL11.glVertex2f(0, 0)
    GL11.glVertex2f(0, mc.displayHeight)
    GL11.glVertex2f(mc.displayWidth, mc.displayHeight)
    GL11.glVertex2f(mc.displayWidth, y + maxHeight)
    GL11.glVertex2f(0, y + maxHeight)
    GL11.glEnd()
    GlStateManager.popMatrix()
    GlStateManager.colorMask(true, true, true, true)

    // Actual rendering.
    var hovered: Option[InteractiveSegment] = None
    var indent = 0
    var currentY = y - yOffset
    val minY = y - lineHeight(renderer)
    val maxY = y + maxHeight + lineHeight(renderer)
    var segment = document
    while (segment != null) {
      val segmentHeight = segment.nextY(indent, maxWidth, renderer)
      if (currentY + segmentHeight >= minY && currentY <= maxY) {
        val result = segment.render(x, currentY, indent, maxWidth, renderer, mouseX, mouseY)
        hovered = hovered.orElse(result)
      }
      currentY += segmentHeight
      indent = segment.nextX(indent, maxWidth, renderer)
      segment = segment.next
    }
    if (mouseX < x || mouseX > x + maxWidth || mouseY < y || mouseY > y + maxHeight) hovered = None
    hovered.foreach(_.notifyHover())

    RenderState.popAttrib()
    GlStateManager.bindTexture(0)

    hovered
  }

  // ----------------------------------------------------------------------- //

  private def HeaderSegment(s: Segment, m: Regex.Match) = new segment.HeaderSegment(s, m.group(2), m.group(1).length)

  private def CodeSegment(s: Segment, m: Regex.Match) = new segment.CodeSegment(s, m.group(2))

  private def LinkSegment(s: Segment, m: Regex.Match) = new segment.LinkSegment(s, m.group(1), m.group(2))

  private def BoldSegment(s: Segment, m: Regex.Match) = new segment.BoldSegment(s, m.group(2))

  private def ItalicSegment(s: Segment, m: Regex.Match) = new segment.ItalicSegment(s, m.group(2))

  private def StrikethroughSegment(s: Segment, m: Regex.Match) = new segment.StrikethroughSegment(s, m.group(1))

  private def ImageSegment(s: Segment, m: Regex.Match) = {
    try Option(api.Manual.imageFor(m.group(2))) match {
      case Some(renderer) => new segment.RenderSegment(s, m.group(1), renderer)
      case _ => new segment.TextSegment(s, "No renderer found for: " + m.group(2))
    } catch {
      case t: Throwable => new segment.TextSegment(s, Option(t.toString).getOrElse("Unknown error."))
    }
  }

  // ----------------------------------------------------------------------- //

  private val segmentTypes = Array(
    """^(#+)\s(.*)""".r -> HeaderSegment _, // headers: # ...
    """(`)(.*?)\1""".r -> CodeSegment _, // code: `...`
    """!\[([^\[]*)\]\(([^\)]+)\)""".r -> ImageSegment _, // images: ![...](...)
    """\[([^\[]+)\]\(([^\)]+)\)""".r -> LinkSegment _, // links: [...](...)
    """(\*\*|__)(\S.*?\S|$)\1""".r -> BoldSegment _, // bold: **...** | __...__
    """(\*|_)(\S.*?\S|$)\1""".r -> ItalicSegment _, // italic: *...* | _..._
    """~~(\S.*?\S|$)~~""".r -> StrikethroughSegment _ // strikethrough: ~~...~~
  )
}
