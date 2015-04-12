package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.Document
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.matching.Regex

private[markdown] class TextSegment(val parent: Segment, val text: String) extends Segment {
  private final val breaks = Set(' ', '.', ',', ':', ';', '!', '?', '_', '=', '-', '+', '*', '/', '\\')
  private final val lists = Set("- ", "* ")
  private lazy val rootPrefix = root.asInstanceOf[TextSegment].text.take(2)

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

  override def nextX(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    if (isLast) return 0
    var currentX = indent
    var chars = text
    if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    while (chars.length > numChars) {
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
      currentX = wrapIndent
    }
    currentX + (stringWidth(chars, renderer) * resolvedScale).toInt
  }

  override def nextY(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    var lines = 0
    var chars = text
    if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    while (chars.length > numChars) {
      lines += 1
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
    }
    if (isLast) lines += 1
    (lines * Document.lineHeight(renderer) * resolvedScale).toInt
  }

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val fontScale = resolvedScale
    var currentX = x + indent
    var currentY = y
    var chars = text
    if (indent == 0) chars = chars.dropWhile(_.isWhitespace)
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    val interactive = findInteractive()
    var hovered: Option[InteractiveSegment] = None
    while (chars.length > 0) {
      val part = chars.take(numChars)
      hovered = hovered.orElse(interactive.fold(None: Option[InteractiveSegment])(_.checkHovered(mouseX, mouseY, currentX, currentY, (stringWidth(part, renderer) * fontScale).toInt, (Document.lineHeight(renderer) * fontScale).toInt)))
      GL11.glPushMatrix()
      GL11.glTranslatef(currentX, currentY, 0)
      GL11.glScalef(fontScale, fontScale, fontScale)
      GL11.glTranslatef(-currentX, -currentY, 0)
      renderer.drawString(resolvedFormat + part, currentX, currentY, resolvedColor)
      GL11.glPopMatrix()
      currentX = x + wrapIndent
      currentY += (Document.lineHeight(renderer) * fontScale).toInt
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
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

  private def maxChars(s: String, maxWidth: Int, maxLineWidth: Int, renderer: FontRenderer): Int = {
    val fontScale = resolvedScale
    var pos = -1
    var lastBreak = -1
    while (pos < s.length) {
      pos += 1
      val width = (stringWidth(s.take(pos), renderer) * fontScale).toInt
      if (width >= maxWidth) {
        if (lastBreak > 0 || stringWidth(s, renderer) <= maxLineWidth || s.exists(breaks.contains)) return lastBreak + 1
        else return pos - 1
      }
      if (pos < s.length && breaks.contains(s.charAt(pos))) lastBreak = pos
    }
    pos
  }

  private def computeWrapIndent(renderer: FontRenderer) = if (lists.contains(rootPrefix)) renderer.getStringWidth(rootPrefix) else 0

  override def toString: String = s"{TextSegment: text = $text}"
}
