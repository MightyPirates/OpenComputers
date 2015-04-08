package li.cil.oc.util

import java.io.InputStream
import javax.imageio.ImageIO

import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

import scala.annotation.tailrec
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
  def parse(document: Iterator[String]): Iterable[Segment] = {
    var segments = document.flatMap(line => Iterable(new TextSegment(null, line), new NewLineSegment())).toArray
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
      val result = segment.render(x, y + currentY - yOffset, currentX, maxWidth, maxHeight - (currentY - yOffset), renderer, mouseX, mouseY)
      hovered = hovered.orElse(result)
      currentY += segment.height(currentX, maxWidth, renderer)
      currentX = segment.width(currentX, maxWidth, renderer)
    }
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

  def lineHeight(renderer: FontRenderer): Int = renderer.FONT_HEIGHT + 1

  // ----------------------------------------------------------------------- //

  trait Segment {
    // Set after construction of document, used for formatting (e.g. newline height).
    private[PseudoMarkdown] var previous: Segment = null

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

    def render(x: Int, y: Int, indent: Int, maxWidth: Int, maxHeight: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = None
  }

  trait InteractiveSegment extends Segment {
    def tooltip: Option[String] = None

    def link: Option[String] = None

    private[PseudoMarkdown] def notifyHover(): Unit = {}

    private[PseudoMarkdown] def checkHovered(mouseX: Int, mouseY: Int, x: Int, y: Int, w: Int, h: Int): Option[InteractiveSegment] = if (mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h) Some(this) else None
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
      var lines = 0
      var chars = text
      var lineChars = maxChars(chars, maxWidth - indent, renderer)
      while (chars.length > lineChars) {
        lines += 1
        chars = chars.drop(lineChars)
        lineChars = maxChars(chars, maxWidth, renderer)
      }
      (lines * lineHeight(renderer) * resolvedScale).toInt
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
      currentX + (stringWidth(chars, renderer) * resolvedScale).toInt
    }

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, maxHeight: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
      val fontScale = resolvedScale
      var currentX = x + indent
      var currentY = y
      var chars = text
      var numChars = maxChars(chars, maxWidth - indent, renderer)
      val interactive = findInteractive()
      var hovered: Option[InteractiveSegment] = None
      while (chars.length > 0 && (currentY - y) < maxHeight) {
        val part = chars.take(numChars)
        hovered = hovered.orElse(interactive.fold(None: Option[InteractiveSegment])(_.checkHovered(mouseX, mouseY, currentX, currentY, (stringWidth(part, renderer) * fontScale).toInt, (lineHeight(renderer) * fontScale).toInt)))
        GL11.glPushMatrix()
        GL11.glTranslatef(currentX, currentY, 0)
        GL11.glScalef(fontScale, fontScale, fontScale)
        GL11.glTranslatef(-currentX, -currentY, 0)
        renderer.drawString(resolvedFormat + part, currentX, currentY, resolvedColor)
        GL11.glPopMatrix()
        currentX = x
        currentY += (lineHeight(renderer) * fontScale).toInt
        chars = chars.drop(numChars)
        numChars = maxChars(chars, maxWidth, renderer)
      }

      hovered
    }

    protected def color = None: Option[Int]

    protected def scale = None: Option[Float]

    protected def format = ""

    protected def stringWidth(s: String, renderer: FontRenderer): Int = renderer.getStringWidth(resolvedFormat + s)

    def resolvedColor: Int = parent match {
      case segment: TextSegment => color.getOrElse(segment.resolvedColor)
      case _ => color.getOrElse(0xDDDDDD)
    }

    def resolvedScale: Float = parent match {
      case segment: TextSegment => scale.getOrElse(segment.resolvedScale)
      case _ => scale.getOrElse(1f)
    }

    def resolvedFormat: String = parent match {
      case segment: TextSegment => segment.resolvedFormat + format
      case _ => format
    }

    @tailrec private def findInteractive(): Option[InteractiveSegment] = this match {
      case segment: InteractiveSegment => Some(segment)
      case _ => parent match {
        case segment: TextSegment => segment.findInteractive()
        case _ => None
      }
    }

    private def maxChars(s: String, maxWidth: Int, renderer: FontRenderer): Int = {
      val fontScale = resolvedScale
      val breaks = Set(' ', '-', '.', '+', '*', '_', '/')
      var pos = 0
      var lastBreak = -1
      while (pos < s.length) {
        pos += 1
        val width = (stringWidth(s.take(pos), renderer) * fontScale).toInt
        if (width >= maxWidth) return lastBreak + 1
        if (pos < s.length && breaks.contains(s.charAt(pos))) lastBreak = pos
      }
      pos
    }

    override def toString: String = s"{TextSegment: text = $text}"
  }

  private class HeaderSegment(parent: Segment, text: String, val level: Int) extends TextSegment(parent, text) {
    private val fontScale = math.max(2, 5 - level) / 2f

    override protected def scale = Some(fontScale)

    override protected def format = EnumChatFormatting.UNDERLINE.toString

    override def toString: String = s"{HeaderSegment: text = $text, level = $level}"
  }

  private class LinkSegment(parent: Segment, text: String, val url: String) extends TextSegment(parent, text) with InteractiveSegment {
    private final val normalColor = 0x66FF66
    private final val hoverColor = 0xAAFFAA
    private final val fadeTime = 500
    private var lastHovered = System.currentTimeMillis() - fadeTime

    override protected def color: Option[Int] = {
      val timeSinceHover = (System.currentTimeMillis() - lastHovered).toInt
      if (timeSinceHover > fadeTime) Some(normalColor)
      else Some(fadeColor(hoverColor, normalColor, timeSinceHover / fadeTime.toFloat))
    }

    override def tooltip: Option[String] = Option(url)

    override def link: Option[String] = Option(url)

    override private[PseudoMarkdown] def notifyHover(): Unit = lastHovered = System.currentTimeMillis()

    private def fadeColor(c1: Int, c2: Int, t: Float): Int = {
      val (r1, g1, b1) = ((c1 >>> 16) & 0xFF, (c1 >>> 8) & 0xFF, c1 & 0xFF)
      val (r2, g2, b2) = ((c2 >>> 16) & 0xFF, (c2 >>> 8) & 0xFF, c2 & 0xFF)
      val (r, g, b) = ((r1 + (r2 - r1) * t).toInt, (g1 + (g2 - g1) * t).toInt, (b1 + (b2 - b1) * t).toInt)
      (r << 16) | (g << 8) | b
    }

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

  private class ImageSegment(val parent: Segment, val title: String, val url: String) extends InteractiveSegment {
    val path = if (url.startsWith("/")) url else "doc/img/" + url
    val location = new ResourceLocation(Settings.resourceDomain, path)
    val texture = {
      val manager = Minecraft.getMinecraft.getTextureManager
      manager.getTexture(location) match {
        case image: ImageTexture => image
        case _ =>
          val image = new ImageTexture(location)
          manager.loadTexture(location, image)
          image
      }
    }

    override def tooltip: Option[String] = Option(title)

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = math.max(lineHeight(renderer), texture.height + 10 - lineHeight(renderer))

    override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = maxWidth

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, maxHeight: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
      Minecraft.getMinecraft.getTextureManager.bindTexture(location)
      val xOffset = (maxWidth - texture.width) / 2
      val yOffset = 4 + (if (indent > 0) lineHeight(renderer) else 0)
      GL11.glColor4f(1, 1, 1, 1)
      GL11.glBegin(GL11.GL_QUADS)
      GL11.glTexCoord2f(0, 0)
      GL11.glVertex2f(x + xOffset, y + yOffset)
      GL11.glTexCoord2f(0, 1)
      GL11.glVertex2f(x + xOffset, y + yOffset + texture.height)
      GL11.glTexCoord2f(1, 1)
      GL11.glVertex2f(x + xOffset + texture.width, y + yOffset + texture.height)
      GL11.glTexCoord2f(1, 0)
      GL11.glVertex2f(x + xOffset + texture.width, y + yOffset)
      GL11.glEnd()

      checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, texture.width, texture.height)
    }

    override def toString: String = s"{ImageSegment: tooltip = $tooltip, url = $url}"
  }

  private class NewLineSegment extends Segment {
    override protected def parent: Segment = null

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = previous match {
      case segment: TextSegment => (lineHeight(renderer) * segment.resolvedScale).toInt
      case _ => lineHeight(renderer)
    }

    override def toString: String = s"{NewLineSegment}"
  }

  private class ImageTexture(val location: ResourceLocation) extends AbstractTexture {
    var width = 0
    var height = 0

    override def loadTexture(manager: IResourceManager): Unit = {
      deleteGlTexture()

      var is: InputStream = null
      try {
        val resource = manager.getResource(location)
        is = resource.getInputStream
        val bi = ImageIO.read(is)
        TextureUtil.uploadTextureImageAllocate(getGlTextureId, bi, false, false)
        width = bi.getWidth
        height = bi.getHeight
      }
      finally {
        Option(is).foreach(_.close())
      }
    }
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
