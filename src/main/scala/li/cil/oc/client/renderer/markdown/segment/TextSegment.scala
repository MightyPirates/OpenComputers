package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.PseudoMarkdown
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.matching.Regex

private[markdown] class TextSegment(protected val parent: Segment, val text: String) extends Segment {
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
    (lines * PseudoMarkdown.lineHeight(renderer) * resolvedScale).toInt
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
      hovered = hovered.orElse(interactive.fold(None: Option[InteractiveSegment])(_.checkHovered(mouseX, mouseY, currentX, currentY, (stringWidth(part, renderer) * fontScale).toInt, (PseudoMarkdown.lineHeight(renderer) * fontScale).toInt)))
      GL11.glPushMatrix()
      GL11.glTranslatef(currentX, currentY, 0)
      GL11.glScalef(fontScale, fontScale, fontScale)
      GL11.glTranslatef(-currentX, -currentY, 0)
      renderer.drawString(resolvedFormat + part, currentX, currentY, resolvedColor)
      GL11.glPopMatrix()
      currentX = x
      currentY += (PseudoMarkdown.lineHeight(renderer) * fontScale).toInt
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
