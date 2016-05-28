package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.Document
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

import scala.collection.mutable
import scala.util.matching.Regex

private[markdown] class TextSegment(val parent: Segment, val text: String) extends BasicTextSegment {
  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    var currentX = x + indent
    var currentY = y
    var chars = text
    if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    var hovered: Option[InteractiveSegment] = None
    while (chars.length > 0) {
      val part = chars.take(numChars)
      hovered = hovered.orElse(resolvedInteractive.fold(None: Option[InteractiveSegment])(_.checkHovered(mouseX, mouseY, currentX, currentY, stringWidth(part, renderer), (Document.lineHeight(renderer) * resolvedScale).toInt)))
      GlStateManager.pushMatrix()
      GlStateManager.translate(currentX, currentY, 0)
      GlStateManager.scale(resolvedScale, resolvedScale, resolvedScale)
      GlStateManager.translate(-currentX, -currentY, 0)
      renderer.drawString(resolvedFormat + part, currentX, currentY, resolvedColor)
      GlStateManager.popMatrix()
      currentX = x + wrapIndent
      currentY += lineHeight(renderer)
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
    }

    hovered
  }

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

  // ----------------------------------------------------------------------- //

  override protected def lineHeight(renderer: FontRenderer): Int = (super.lineHeight(renderer) * resolvedScale).toInt

  override protected def stringWidth(s: String, renderer: FontRenderer): Int = (renderer.getStringWidth(resolvedFormat + s) * resolvedScale).toInt

  // ----------------------------------------------------------------------- //

  protected def color = None: Option[Int]

  protected def scale = None: Option[Float]

  protected def format = ""

  private def resolvedColor: Int = color.getOrElse(parent match {
    case segment: TextSegment => segment.resolvedColor
    case _ => 0xDDDDDD
  })

  private def resolvedScale: Float = parent match {
    case segment: TextSegment => scale.getOrElse(1f) * segment.resolvedScale
    case _ => 1f
  }

  private def resolvedFormat: String = parent match {
    case segment: TextSegment => segment.resolvedFormat + format
    case _ => format
  }

  private lazy val resolvedInteractive: Option[InteractiveSegment] = this match {
    case segment: InteractiveSegment => Some(segment)
    case _ => parent match {
      case segment: TextSegment => segment.resolvedInteractive
      case _ => None
    }
  }
}
