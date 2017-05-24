package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.Document
import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.client.gui.FontRenderer

trait BasicTextSegment extends Segment {
  protected final val breaks = Set(' ', '.', ',', ':', ';', '!', '?', '_', '=', '-', '+', '*', '/', '\\')
  protected final val lists = Set("- ", "* ")
  protected lazy val rootPrefix = root.asInstanceOf[TextSegment].text.take(2)

  override def nextX(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    if (isLast) return 0
    var currentX = indent
    var chars = text
    if (ignoreLeadingWhitespace && indent == 0) chars = chars.dropWhile(_.isWhitespace)
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    while (chars.length > numChars) {
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
      currentX = wrapIndent
    }
    currentX + stringWidth(chars, renderer)
  }

  override def nextY(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    var lines = 0
    var chars = text
    if (ignoreLeadingWhitespace && indent == 0) chars = chars.dropWhile(_.isWhitespace)
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    while (chars.length > numChars) {
      lines += 1
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
    }
    if (isLast) lines += 1
    lines * lineHeight(renderer)
  }

  override def toString(format: MarkupFormat.Value): String = text

  // ----------------------------------------------------------------------- //

  protected def text: String

  protected def ignoreLeadingWhitespace: Boolean = true

  protected def lineHeight(renderer: FontRenderer): Int = Document.lineHeight(renderer)

  protected def stringWidth(s: String, renderer: FontRenderer): Int

  protected def maxChars(s: String, maxWidth: Int, maxLineWidth: Int, renderer: FontRenderer): Int = {
    var pos = -1
    var lastBreak = -1
    val fullWidth = stringWidth(s, renderer)
    while (pos < s.length) {
      pos += 1
      val width = stringWidth(s.take(pos), renderer)
      if (width >= maxWidth) {
        if (lastBreak > 0 || fullWidth <= maxLineWidth || s.exists(breaks.contains))
          if (maxWidth == maxLineWidth && fullWidth == maxLineWidth && !s.exists(breaks.contains)) return s.length
          else if(lastBreak == -1)
          {
            //lastbreak = -1, then return pos - 1
            //if return lastBreak + 1,we will run into infinite loop
            return pos - 1
          }
          else {
            return lastBreak + 1 //if we got a long sentence without breaks ,lastbreak still -1
          }
        else return pos - 1
      }
      if (pos < s.length && breaks.contains(s.charAt(pos))) lastBreak = pos
    }
    pos
  }

  protected def computeWrapIndent(renderer: FontRenderer) = if (lists.contains(rootPrefix)) renderer.getStringWidth(rootPrefix) else 0
}
