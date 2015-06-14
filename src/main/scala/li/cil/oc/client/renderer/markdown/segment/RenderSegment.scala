package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.renderer.markdown.Document
import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

private[markdown] class RenderSegment(val parent: Segment, val title: String, val imageRenderer: ImageRenderer) extends InteractiveSegment {
  var lastX = 0
  var lastY = 0

  override def tooltip: Option[String] = imageRenderer match {
    case interactive: InteractiveImageRenderer => Option(interactive.getTooltip(title))
    case _ => Option(title)
  }

  override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = imageRenderer match {
    case interactive: InteractiveImageRenderer => interactive.onMouseClick(mouseX - lastX, mouseY - lastY)
    case _ => false
  }

  private def scale(maxWidth: Int) = math.min(1f, maxWidth / imageRenderer.getWidth.toFloat)

  def imageWidth(maxWidth: Int) = math.min(maxWidth, imageRenderer.getWidth)

  def imageHeight(maxWidth: Int) = math.ceil(imageRenderer.getHeight * scale(maxWidth)).toInt + 4

  override def nextY(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = imageHeight(maxWidth) + (if (indent > 0) Document.lineHeight(renderer) else 0)

  override def nextX(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = 0

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val width = imageWidth(maxWidth)
    val height = imageHeight(maxWidth)
    val xOffset = (maxWidth - width) / 2
    val yOffset = 2 + (if (indent > 0) Document.lineHeight(renderer) else 0)
    val s = scale(maxWidth)

    lastX = x + xOffset
    lastY = y + yOffset

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

    imageRenderer.render(mouseX - x, mouseY - y)

    GL11.glDisable(GL11.GL_BLEND)
    GL11.glDisable(GL11.GL_ALPHA_TEST)
    GL11.glDisable(GL11.GL_LIGHTING)

    GL11.glPopMatrix()

    hovered
  }

  override def toString(format: MarkupFormat.Value): String = format match {
    case MarkupFormat.Markdown => s"![$title]($imageRenderer)"
    case MarkupFormat.IGWMod => "(Sorry, images only work in the OpenComputers manual for now.)" // TODO
  }
}
