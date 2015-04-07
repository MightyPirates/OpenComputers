package li.cil.oc.util

import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11

import scala.collection.mutable
import scala.util.matching.Regex

/**
 * Primitive Markdown parser, only supports a very small subset. Used for
 * parsing documentation into segments, to be displayed in a GUI somewhere.
 */
object PseudoMarkdown {
  /**
   * Parses a plain text document into a list of segments.
   */
  def parse(document: String): Iterable[Segment] = {
    var segments = document.lines.flatMap(line => Iterable(new TextSegment(null, line), new NewLineSegment())).toArray
    for ((pattern, factory) <- segmentTypes) {
      segments = segments.flatMap(_.refine(pattern, factory))
    }
    segments
  }

  /**
   * Renders a list of segments and tooltips if a segment with a tooltip is hovered.
   * Returns a link address if a link is hovered.
   */
  def render(document: Iterable[Segment], x: Int, y: Int, maxWidth: Int, height: Int, offset: Int, renderer: FontRenderer): Option[String] = {
    var currentX = 0
    var currentY = 0
    for (segment <- document) {
      if (currentY >= offset) {
        segment.render(x, y + currentY, currentX, maxWidth, renderer)
      }
      currentY += segment.height(currentX, maxWidth, renderer) - renderer.FONT_HEIGHT
      currentX = segment.width(currentX, maxWidth, renderer)
    }

    None
  }

  // ----------------------------------------------------------------------- //

  trait Segment {
    // Used when rendering, to compute the style of a nested segment.
    protected def parent: Segment

    // Used during construction, checks a segment for inner segments.
    private[PseudoMarkdown] def refine(pattern: Regex, factory: (Segment, Regex.Match) => Segment): Iterable[Segment] = Iterable(this)

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

    def render(x: Int, y: Int, indent: Int, width: Int, renderer: FontRenderer): Unit = {}
  }

  // ----------------------------------------------------------------------- //

  private class TextSegment(protected val parent: Segment, val text: String) extends Segment {
    override def refine(pattern: Regex, factory: (Segment, Regex.Match) => Segment): Iterable[Segment] = {
      val result = mutable.Buffer.empty[Segment]

      // Keep track of last matches end, to generate plain text segments.
      var textStart = 0
      for (m <- pattern.findAllMatchIn(text)) {
        // Create segment for leading plain text.
        if (m.start > textStart) {
          result += new TextSegment(this, text.substring(textStart, m.start))
        }
        textStart = m.end

        // Create segment for formatted text.
        result += factory(this, m)
      }

      // Create segment for remaining plain text.
      if (textStart == 0) {
        result += this
      }
      else if (textStart < text.length) {
        result += new TextSegment(this, text.substring(textStart))
      }
      result
    }

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
      var lines = 1
      var chars = text
      var lineChars = maxChars(chars, maxWidth - indent, renderer)
      while (chars.length > lineChars) {
        lines += 1
        chars = chars.drop(lineChars)
        lineChars = maxChars(chars, maxWidth, renderer)
      }
      lines * renderer.FONT_HEIGHT
    }

    override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
      var currentX = indent
      var chars = text
      var lineChars = maxChars(chars, maxWidth - indent, renderer)
      while (chars.length > lineChars) {
        chars = chars.drop(lineChars)
        lineChars = maxChars(chars, maxWidth, renderer)
        currentX = 0
      }
      currentX + renderer.getStringWidth(fullFormat + chars)
    }

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer): Unit = {
      var currentX = x + indent
      var currentY = y
      var chars = text
      var numChars = maxChars(chars, maxWidth - indent, renderer)
      while (chars.length > 0) {
        renderer.drawString(fullFormat + chars.take(numChars), currentX, currentY, 0xFFFFFF)
        currentX = x
        currentY += renderer.FONT_HEIGHT
        chars = chars.drop(numChars)
        numChars = maxChars(chars, maxWidth, renderer)
      }
    }

    protected def format = ""

    protected def stringWidth(s: String, renderer: FontRenderer): Int = renderer.getStringWidth(s)

    private def fullFormat = parent match {
      case segment: TextSegment => segment.format + format
      case _ => format
    }

    private def maxChars(s: String, maxWidth: Int, renderer: FontRenderer): Int = {
      val breaks = Set(' ', '-', '.', '+', '*', '_', '/')
      var pos = 1
      var lastBreak = -1
      while (pos < s.length) {
        val width = stringWidth(fullFormat + s.take(pos), renderer)
        if (breaks.contains(s.charAt(pos))) lastBreak = pos
        if (width > maxWidth) return lastBreak + 1
        pos += 1
      }
      pos
    }

    override def toString: String = s"{TextSegment: text = $text}"
  }

  private class HeaderSegment(parent: Segment, text: String, val level: Int) extends TextSegment(parent, text) {
    private def scale = math.max(2, 5 - level) / 2f

    override protected def format = EnumChatFormatting.BOLD.toString

    override protected def stringWidth(s: String, renderer: FontRenderer): Int = (super.stringWidth(s, renderer) * scale).toInt

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = (super.height(indent, maxWidth, renderer) * scale).toInt

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer): Unit = {
      GL11.glPushMatrix()
      GL11.glTranslatef(x, y, 0)
      GL11.glScalef(scale, scale, scale)
      GL11.glTranslatef(-x, -y, 0)
      super.render(x, y, indent, maxWidth, renderer)
      GL11.glPopMatrix()
    }

    override def toString: String = s"{HeaderSegment: text = $text, level = $level}"
  }

  private class LinkSegment(parent: Segment, text: String, val url: String) extends TextSegment(parent, text) {
    override def toString: String = s"{LinkSegment: text = $text, url = $url}"
  }

  private class BoldSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
    override protected def format = EnumChatFormatting.BOLD.toString

    override def toString: String = s"{BoldSegment: text = $text}"
  }

  private class ItalicSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
    override protected def format = EnumChatFormatting.ITALIC.toString

    override def toString: String = s"{ItalicSegment: text = $text}"
  }

  private class StrikethroughSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
    override protected def format = EnumChatFormatting.STRIKETHROUGH.toString

    override def toString: String = s"{StrikethroughSegment: text = $text}"
  }

  private class ImageSegment(val parent: Segment, val tooltip: String, val url: String) extends Segment {
    override def toString: String = s"{ImageSegment: tooltip = $tooltip, url = $url}"
  }

  private class NewLineSegment extends Segment {
    override protected def parent: Segment = null

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = renderer.FONT_HEIGHT * 2

    override def toString: String = s"{NewLineSegment}"
  }

  // ----------------------------------------------------------------------- //

  private def HeaderSegment(s: Segment, m: Regex.Match) = new HeaderSegment(s, m.group(2), m.group(1).length)

  private def LinkSegment(s: Segment, m: Regex.Match) = new LinkSegment(s, m.group(1), m.group(2))

  private def BoldSegment(s: Segment, m: Regex.Match) = new BoldSegment(s, m.group(2))

  private def ItalicSegment(s: Segment, m: Regex.Match) = new ItalicSegment(s, m.group(2))

  private def StrikethroughSegment(s: Segment, m: Regex.Match) = new StrikethroughSegment(s, m.group(1))

  private def ImageSegment(s: Segment, m: Regex.Match) = new ImageSegment(s, m.group(1), m.group(2))

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
