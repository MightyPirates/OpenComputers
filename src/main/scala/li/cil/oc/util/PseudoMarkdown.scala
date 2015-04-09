package li.cil.oc.util

import java.io.InputStream
import javax.imageio.ImageIO

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.Manual
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.OreDictionary
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12

import scala.annotation.tailrec
import scala.collection.convert.WrapAsScala._
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
  def parse(document: Iterable[String]): Iterable[Segment] = {
    var segments = document.flatMap(line => Iterable(new TextSegment(null, Option(line).getOrElse("")), new NewLineSegment())).toArray
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

    def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = None
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
      if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
      var lineChars = maxChars(chars, maxWidth - indent, renderer)
      while (chars.length > lineChars) {
        lines += 1
        chars = chars.drop(lineChars).dropWhile(_.isWhitespace)
        lineChars = maxChars(chars, maxWidth, renderer)
      }
      (lines * lineHeight(renderer) * resolvedScale).toInt
    }

    override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
      var currentX = indent
      var chars = text
      if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
      var lineChars = maxChars(chars, maxWidth - indent, renderer)
      while (chars.length > lineChars) {
        chars = chars.drop(lineChars).dropWhile(_.isWhitespace)
        lineChars = maxChars(chars, maxWidth, renderer)
        currentX = 0
      }
      currentX + (stringWidth(chars, renderer) * resolvedScale).toInt
    }

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
      val fontScale = resolvedScale
      var currentX = x + indent
      var currentY = y
      var chars = text
      if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
      var numChars = maxChars(chars, maxWidth - indent, renderer)
      val interactive = findInteractive()
      var hovered: Option[InteractiveSegment] = None
      while (chars.length > 0 && (currentY - y) < maxY) {
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
        chars = chars.drop(numChars).dropWhile(_.isWhitespace)
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

  private class RendererSegment(val parent: Segment, val title: String, val imageRenderer: ImageRenderer) extends InteractiveSegment {
    override def tooltip: Option[String] = Option(title)

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = math.max(lineHeight(renderer), imageRenderer.getHeight + 10 - lineHeight(renderer))

    override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = maxWidth

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
      val xOffset = (maxWidth - imageRenderer.getWidth) / 2
      val yOffset = 4 + (if (indent > 0) lineHeight(renderer) else 0)
      val maskLow = math.max(minY - y - yOffset - 1, 0)
      val maskHigh = math.max(maskLow, math.min(imageRenderer.getHeight, maxY - 3))

      GL11.glPushMatrix()
      GL11.glTranslatef(x + xOffset, y + yOffset, 0)

      // TODO hacky as shit, find a better way
      // maybe render a plane in the *foreground*, then one in the actual layer (with force updating depth buffer),
      // then draw normally?
      GL11.glColor4f(0.1f, 0.1f, 0.1f, 1)
      GL11.glTranslatef(0, 0, 400)
      GL11.glDepthFunc(GL11.GL_LEQUAL)
      GL11.glDisable(GL11.GL_TEXTURE_2D)
      GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE)
      GL11.glEnable(GL11.GL_COLOR_MATERIAL)
      GL11.glDepthMask(true)
      GL11.glColorMask(false, false, false, false)
      GL11.glBegin(GL11.GL_QUADS)
      GL11.glVertex2f(0, maskLow)
      GL11.glVertex2f(0, maskHigh)
      GL11.glVertex2f(imageRenderer.getWidth, maskHigh)
      GL11.glVertex2f(imageRenderer.getWidth, maskLow)
      GL11.glEnd()
      GL11.glTranslatef(0, 0, -400)
      GL11.glDepthFunc(GL11.GL_GEQUAL)
      GL11.glEnable(GL11.GL_TEXTURE_2D)
      GL11.glDisable(GL11.GL_COLOR_MATERIAL)
      GL11.glDepthMask(false)
      GL11.glColorMask(true, true, true, true)

      GL11.glColor4f(1, 1, 1, 1)

      imageRenderer.render(maxWidth)

      GL11.glPopMatrix()

      checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, imageRenderer.getWidth, imageRenderer.getHeight)
    }

    override def toString: String = s"{RendererSegment: title = $title, imageRenderer = $imageRenderer}"
  }

  private class ItemStackRenderer(val stacks: Array[ItemStack]) extends ImageRenderer {
    final val cycleSpeed = 1000

    override def getWidth = 32

    override def getHeight = 32

    override def render(maxWidth: Int): Unit = {
      val mc = Minecraft.getMinecraft
      val index = (System.currentTimeMillis() % (cycleSpeed * stacks.length)).toInt / cycleSpeed
      val stack = stacks(index)

      GL11.glScalef(getWidth / 16, getHeight / 16, getWidth / 16)
      GL11.glEnable(GL12.GL_RESCALE_NORMAL)
      RenderHelper.enableGUIStandardItemLighting()
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240)
      RenderItem.getInstance.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager, stack, 0, 0)
      RenderHelper.disableStandardItemLighting()
    }
  }

  private class ImageSegment(val parent: Segment, val title: String, val url: String) extends InteractiveSegment {
    val path = if (url.startsWith("/")) url else "doc/" + url
    val location = new ResourceLocation(Settings.resourceDomain, path)
    val texture = {
      val manager = Minecraft.getMinecraft.getTextureManager
      manager.getTexture(location) match {
        case image: ImageTexture => image
        case other =>
          if (other != null && other.getGlTextureId != -1) {
            TextureUtil.deleteTexture(other.getGlTextureId)
          }
          val image = new ImageTexture(location)
          manager.loadTexture(location, image)
          image
      }
    }

    def scale(maxWidth: Int) = math.min(1f, maxWidth / texture.width.toFloat)

    override def tooltip: Option[String] = Option(title)

    override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
      val s = scale(maxWidth)
      // This 2/s feels super-hacky, because I have no idea why it works >_>
      math.max(lineHeight(renderer), (texture.height * s + 2 / s).toInt - lineHeight(renderer))
    }

    override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = maxWidth

    override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
      val s = scale(maxWidth)
      val (renderWidth, renderHeight) = ((texture.width * s).toInt, (texture.height * s).toInt)
      val xOffset = (maxWidth - renderWidth) / 2
      val yOffset = 4 + (if (indent > 0) lineHeight(renderer) else 0)
      Minecraft.getMinecraft.getTextureManager.bindTexture(location)
      GL11.glColor4f(1, 1, 1, 1)
      GL11.glBegin(GL11.GL_QUADS)
      GL11.glTexCoord2f(0, 0)
      GL11.glVertex2f(x + xOffset, y + yOffset)
      GL11.glTexCoord2f(0, 1)
      GL11.glVertex2f(x + xOffset, y + yOffset + renderHeight)
      GL11.glTexCoord2f(1, 1)
      GL11.glVertex2f(x + xOffset + renderWidth, y + yOffset + renderHeight)
      GL11.glTexCoord2f(1, 0)
      GL11.glVertex2f(x + xOffset + renderWidth, y + yOffset)
      GL11.glEnd()

      checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, renderWidth, renderHeight)
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

  object ItemRenderProvider extends ImageProvider {
    override def getImage(href: String): ImageRenderer = {
      val splitIndex = href.lastIndexOf('@')
      val (name, optMeta) = if (splitIndex > 0) href.splitAt(splitIndex) else (href, "")
      val meta = if (Strings.isNullOrEmpty(optMeta)) 0 else Integer.parseInt(optMeta.drop(1))
      Item.itemRegistry.getObject(name) match {
        case item: Item => new ItemStackRenderer(Array(new ItemStack(item, 1, meta)))
        case _ => null
      }
    }
  }

  object BlockRenderProvider extends ImageProvider {
    override def getImage(href: String): ImageRenderer = {
      val splitIndex = href.lastIndexOf('@')
      val (name, optMeta) = if (splitIndex > 0) href.splitAt(splitIndex) else (href, "")
      val meta = if (Strings.isNullOrEmpty(optMeta)) 0 else Integer.parseInt(optMeta.drop(1))
      Block.blockRegistry.getObject(name) match {
        case block: Block => new ItemStackRenderer(Array(new ItemStack(block, 1, meta)))
        case _ => null
      }
    }
  }

  object OreDictRenderProvider extends ImageProvider {
    override def getImage(desc: String): ImageRenderer = {
      val stacks = OreDictionary.getOres(desc)
      if (stacks != null && stacks.nonEmpty) new ItemStackRenderer(stacks.toArray(new Array[ItemStack](stacks.size())))
      else null
    }
  }

  // ----------------------------------------------------------------------- //

  private def HeaderSegment(s: Segment, m: Regex.Match) = new HeaderSegment(s, m.group(2), m.group(1).length)

  private def LinkSegment(s: Segment, m: Regex.Match) = new LinkSegment(s, m.group(1), m.group(2))

  private def BoldSegment(s: Segment, m: Regex.Match) = new BoldSegment(s, m.group(2))

  private def ItalicSegment(s: Segment, m: Regex.Match) = new ItalicSegment(s, m.group(2))

  private def StrikethroughSegment(s: Segment, m: Regex.Match) = new StrikethroughSegment(s, m.group(1))

  private def ImageSegment(s: Segment, m: Regex.Match) = {
    try Option(Manual.imageFor(m.group(2))) match {
      case Some(renderer) => new RendererSegment(s, m.group(1), renderer)
      case _ => new ImageSegment(s, m.group(1), m.group(2))
    } catch {
      case t: Throwable => new TextSegment(s, Option(t.toString).getOrElse("Unknown error."))
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
