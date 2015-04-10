package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.renderer.markdown.Document
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

private[markdown] class RenderSegment(val parent: Segment, val title: String, val imageRenderer: ImageRenderer) extends InteractiveSegment {
  override def tooltip: Option[String] = Option(title)

  private def scale(maxWidth: Int) = math.min(1f, maxWidth / imageRenderer.getWidth.toFloat)

  def imageWidth(maxWidth: Int) = math.min(maxWidth, imageRenderer.getWidth)

  def imageHeight(maxWidth: Int) = (imageRenderer.getHeight * scale(maxWidth)).toInt

  override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = imageWidth(maxWidth)

  override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    math.max(0, imageHeight(maxWidth)) + (if (indent > 0) Document.lineHeight(renderer) else 0)
  }

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val xOffset = (maxWidth - imageWidth(maxWidth)) / 2
    val yOffset = 4 + (if (indent > 0) Document.lineHeight(renderer) else 0)
    val s = scale(maxWidth)

    GL11.glColor4f(1, 1, 1, 1)
    GL11.glPushMatrix()
    GL11.glTranslatef(x + xOffset, y + yOffset, 0)
    GL11.glScalef(s, s, s)
    imageRenderer.render()
    GL11.glPopMatrix()

    checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, imageWidth(maxWidth), imageHeight(maxWidth))
  }

  override def toString: String = s"{RendererSegment: title = $title, imageRenderer = $imageRenderer}"
}
