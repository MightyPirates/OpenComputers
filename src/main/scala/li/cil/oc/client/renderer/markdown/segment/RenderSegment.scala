package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.renderer.markdown.Document
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

private[markdown] class RenderSegment(val parent: Segment, val title: String, val imageRenderer: ImageRenderer) extends InteractiveSegment {
  override def tooltip: Option[String] = Option(title)

  override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = math.max(Document.lineHeight(renderer), imageRenderer.getHeight + 10 - Document.lineHeight(renderer))

  override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = maxWidth

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val xOffset = (maxWidth - imageRenderer.getWidth) / 2
    val yOffset = 4 + (if (indent > 0) Document.lineHeight(renderer) else 0)

    GL11.glPushMatrix()
    GL11.glTranslatef(x + xOffset, y + yOffset, 0)
    GL11.glColor4f(1, 1, 1, 1)

    imageRenderer.render(maxWidth)

    GL11.glPopMatrix()

    checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, imageRenderer.getWidth, imageRenderer.getHeight)
  }

  override def toString: String = s"{RendererSegment: title = $title, imageRenderer = $imageRenderer}"
}
