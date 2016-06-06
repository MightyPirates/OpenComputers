package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager

private[markdown] class CodeSegment(val parent: Segment, val text: String) extends BasicTextSegment {
  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    TextBufferRenderCache.renderer.generateChars(text.toCharArray)

    var currentX = x + indent
    var currentY = y
    var chars = text
    val wrapIndent = computeWrapIndent(renderer)
    var numChars = maxChars(chars, maxWidth - indent, maxWidth - wrapIndent, renderer)
    while (chars.length > 0) {
      val part = chars.take(numChars)
      GlStateManager.color(0.75f, 0.8f, 1, 1)
      TextBufferRenderCache.renderer.drawString(part, currentX, currentY)
      currentX = x + wrapIndent
      currentY += lineHeight(renderer)
      chars = chars.drop(numChars).dropWhile(_.isWhitespace)
      numChars = maxChars(chars, maxWidth - wrapIndent, maxWidth - wrapIndent, renderer)
    }

    None
  }

  override protected def ignoreLeadingWhitespace: Boolean = false

  override protected def stringWidth(s: String, renderer: FontRenderer): Int = s.length * TextBufferRenderCache.renderer.charRenderWidth

  override def toString(format: MarkupFormat.Value): String = format match {
    case MarkupFormat.Markdown => s"`$text`"
    case MarkupFormat.IGWMod => s"[prefix{1}]$text [prefix{}]"
  }
}
