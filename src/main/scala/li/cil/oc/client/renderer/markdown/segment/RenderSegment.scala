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
    val width = imageWidth(maxWidth)
    val height = imageHeight(maxWidth)
    val xOffset = (maxWidth - width) / 2
    val yOffset = 4 + (if (indent > 0) Document.lineHeight(renderer) else 0)
    val s = scale(maxWidth)

    val hovered = checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, width, height)

    GL11.glPushMatrix()
    GL11.glTranslatef(x + xOffset, y + yOffset, 0)
    GL11.glScalef(s, s, s)

    GL11.glEnable(GL11.GL_BLEND)
    GL11.glEnable(GL11.GL_ALPHA_TEST)

    if (hovered.isDefined) {
      GL11.glColor4f(1, 1, 1, 0.15f)
      GL11.glDisable(GL11.GL_TEXTURE_2D)
      GL11.glBegin(GL11.GL_QUADS)
      GL11.glVertex2f(0, 0)
      GL11.glVertex2f(0, imageRenderer.getHeight)
      GL11.glVertex2f(imageRenderer.getWidth, imageRenderer.getHeight)
      GL11.glVertex2f(imageRenderer.getWidth, 0)
      GL11.glEnd()
      GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    GL11.glColor4f(1, 1, 1, 1)

    imageRenderer.render()

    GL11.glDisable(GL11.GL_BLEND)
    GL11.glDisable(GL11.GL_ALPHA_TEST)
    GL11.glDisable(GL11.GL_LIGHTING)

    GL11.glPopMatrix()

    hovered
  }

  override def toString: String = s"{RendererSegment: title = $title, imageRenderer = $imageRenderer}"
}
