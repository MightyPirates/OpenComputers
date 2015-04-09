package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.renderer.markdown.PseudoMarkdown
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.opengl.GL11

private[markdown] class RenderSegment(val parent: Segment, val title: String, val imageRenderer: ImageRenderer) extends InteractiveSegment {
  override def tooltip: Option[String] = Option(title)

  override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = math.max(PseudoMarkdown.lineHeight(renderer), imageRenderer.getHeight + 10 - PseudoMarkdown.lineHeight(renderer))

  override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = maxWidth

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val xOffset = (maxWidth - imageRenderer.getWidth) / 2
    val yOffset = 4 + (if (indent > 0) PseudoMarkdown.lineHeight(renderer) else 0)
    val maskLow = math.max(minY - y - yOffset - 1, 0)
    val maskHigh = math.max(maskLow, math.min(imageRenderer.getHeight, maxY - 3))

    GL11.glPushMatrix()
    GL11.glTranslatef(x + xOffset, y + yOffset, 0)

    // TODO hacky as shit, find a better way
    // maybe render a plane in the *foreground*, then one in the actual layer (with force updating depth buffer),
    // then draw normally?
    GL11.glColor4f(0.1f, 0.1f, 0.1f, 1)
    GL11.glTranslatef(0, 0, 400)
    GL11.glDepthFunc(GL11.GL_LEQUAL)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE)
    GL11.glEnable(GL11.GL_COLOR_MATERIAL)
    GL11.glDepthMask(true)
    GL11.glColorMask(false, false, false, false)
    GL11.glBegin(GL11.GL_QUADS)
    GL11.glVertex2f(0, maskLow)
    GL11.glVertex2f(0, maskHigh)
    GL11.glVertex2f(imageRenderer.getWidth, maskHigh)
    GL11.glVertex2f(imageRenderer.getWidth, maskLow)
    GL11.glEnd()
    GL11.glTranslatef(0, 0, -400)
    GL11.glDepthFunc(GL11.GL_GEQUAL)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glDisable(GL11.GL_COLOR_MATERIAL)
    GL11.glDepthMask(false)
    GL11.glColorMask(true, true, true, true)

    GL11.glColor4f(1, 1, 1, 1)

    imageRenderer.render(maxWidth)

    GL11.glPopMatrix()

    checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, imageRenderer.getWidth, imageRenderer.getHeight)
  }

  override def toString: String = s"{RendererSegment: title = $title, imageRenderer = $imageRenderer}"
}
