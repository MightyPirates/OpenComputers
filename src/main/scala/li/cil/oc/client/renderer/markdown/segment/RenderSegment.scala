package li.cil.oc.client.renderer.markdown.segment

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.renderer.markdown.Document
import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.util.math.vector.Vector4f
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

  override def render(stack: MatrixStack, x: Int, y: Int, indent: Int, maxWidth: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val width = imageWidth(maxWidth)
    val height = imageHeight(maxWidth)
    val xOffset = (maxWidth - width) / 2
    val yOffset = 2 + (if (indent > 0) Document.lineHeight(renderer) else 0)
    val s = scale(maxWidth)

    lastX = x + xOffset
    lastY = y + yOffset

    val hovered = checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, width, height)

    stack.pushPose()
    stack.translate(x + xOffset, y + yOffset, 0)
    stack.scale(s, s, s)

    RenderSystem.enableBlend()
    RenderSystem.enableAlphaTest()

    if (hovered.isDefined) {
      RenderSystem.color4f(1, 1, 1, 0.15f)
      RenderSystem.disableTexture()
      GL11.glBegin(GL11.GL_QUADS)
      val matrix = stack.last.pose
      val vec = new Vector4f(0, 0, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      vec.set(0, imageRenderer.getHeight, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      vec.set(imageRenderer.getWidth, imageRenderer.getHeight, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      vec.set(imageRenderer.getWidth, 0, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      GL11.glEnd()
      RenderSystem.enableTexture()
    }

    RenderSystem.color4f(1, 1, 1, 1)

    imageRenderer.render(stack, mouseX - x, mouseY - y)

    RenderSystem.disableBlend()
    RenderSystem.disableAlphaTest()
    RenderSystem.disableLighting()

    stack.popPose()

    hovered
  }

  override def toString(format: MarkupFormat.Value): String = format match {
    case MarkupFormat.Markdown => s"![$title]($imageRenderer)"
    case MarkupFormat.IGWMod => "(Sorry, images only work in the OpenComputers manual for now.)" // TODO
  }
}
