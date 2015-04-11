package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.markdown.Document
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

private[markdown] class CodeSegment(protected val parent: Segment, val text: String) extends Segment {
  final val breaks = Set(' ', '.', ',', ':', ';', '!', '?', '_', '=', '-', '+', '*', '/', '\\', ')', '\'', '"')

  override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    var lines = 0
    var chars = text
    var lineChars = maxChars(chars, maxWidth - indent, maxWidth)
    while (chars.length > lineChars) {
      lines += 1
      chars = chars.drop(lineChars).dropWhile(_.isWhitespace)
      lineChars = maxChars(chars, maxWidth, maxWidth)
    }
    lines * Document.lineHeight(renderer)
  }

  override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    var currentX = indent
    var chars = text
    var lineChars = maxChars(chars, maxWidth - indent, maxWidth)
    while (chars.length > lineChars) {
      chars = chars.drop(lineChars).dropWhile(_.isWhitespace)
      lineChars = maxChars(chars, maxWidth, maxWidth)
      currentX = 1
    }
    currentX + stringWidth(chars)
  }

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    TextBufferRenderCache.renderer.generateChars(text.toCharArray)

    var currentX = x + indent
    var currentY = y
    var chars = text
    var numChars = maxChars(chars, maxWidth - indent, maxWidth)
    while (chars.length > 0 && (currentY - y) < maxY) {
      val part = chars.take(numChars)
      GL11.glColor4f(0.75f, 0.8f, 1, 1)
      TextBufferRenderCache.renderer.drawString(part, currentX, currentY)
      currentX = x
      currentY += Document.lineHeight(renderer)
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth, maxWidth)
    }

    None
  }

  private def stringWidth(s: String): Int = s.length * TextBufferRenderCache.renderer.charRenderWidth

  private def maxChars(s: String, maxWidth: Int, maxLineWidth: Int): Int = {
    var pos = 0
    var lastBreak = -1
    while (pos < s.length) {
      pos += 1
      val width = stringWidth(s.take(pos))
      if (width >= maxWidth) {
        if (lastBreak > 0 || stringWidth(s) <= maxLineWidth || s.exists(breaks.contains)) return lastBreak + 1
        else return pos - 1
      }
      if (pos < s.length && breaks.contains(s.charAt(pos))) lastBreak = pos
    }
    pos
  }

  override def toString: String = s"{CodeSegment: text = $text}"
}
