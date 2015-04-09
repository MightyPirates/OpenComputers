package li.cil.oc.client.renderer.markdown.segment

import java.io.InputStream
import javax.imageio.ImageIO

import li.cil.oc.Settings
import li.cil.oc.client.renderer.markdown.PseudoMarkdown
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

private[markdown] class ImageSegment(val parent: Segment, val title: String, val url: String) extends InteractiveSegment {
  private val path = if (url.startsWith("/")) url else "doc/" + url
  private val location = new ResourceLocation(Settings.resourceDomain, path)
  private val texture = {
    val manager = Minecraft.getMinecraft.getTextureManager
    manager.getTexture(location) match {
      case image: ImageTexture => image
      case other =>
        if (other != null && other.getGlTextureId != -1) {
          TextureUtil.deleteTexture(other.getGlTextureId)
        }
        val image = new ImageTexture(location)
        manager.loadTexture(location, image)
        image
    }
  }

  private def scale(maxWidth: Int) = math.min(1f, maxWidth / texture.width.toFloat)

  override def tooltip: Option[String] = Option(title)

  override def height(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = {
    val s = scale(maxWidth)
    // This 2/s feels super-hacky, because I have no idea why it works >_>
    math.max(PseudoMarkdown.lineHeight(renderer), (texture.height * s + 2 / s).toInt - PseudoMarkdown.lineHeight(renderer))
  }

  override def width(indent: Int, maxWidth: Int, renderer: FontRenderer): Int = maxWidth

  override def render(x: Int, y: Int, indent: Int, maxWidth: Int, minY: Int, maxY: Int, renderer: FontRenderer, mouseX: Int, mouseY: Int): Option[InteractiveSegment] = {
    val s = scale(maxWidth)
    val (renderWidth, renderHeight) = ((texture.width * s).toInt, (texture.height * s).toInt)
    val xOffset = (maxWidth - renderWidth) / 2
    val yOffset = 4 + (if (indent > 0) PseudoMarkdown.lineHeight(renderer) else 0)

    Minecraft.getMinecraft.getTextureManager.bindTexture(location)
    GL11.glColor4f(1, 1, 1, 1)
    GL11.glBegin(GL11.GL_QUADS)
    GL11.glTexCoord2f(0, 0)
    GL11.glVertex2f(x + xOffset, y + yOffset)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex2f(x + xOffset, y + yOffset + renderHeight)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex2f(x + xOffset + renderWidth, y + yOffset + renderHeight)
    GL11.glTexCoord2f(1, 0)
    GL11.glVertex2f(x + xOffset + renderWidth, y + yOffset)
    GL11.glEnd()

    checkHovered(mouseX, mouseY, x + xOffset, y + yOffset, renderWidth, renderHeight)
  }

  override def toString: String = s"{ImageSegment: tooltip = $tooltip, url = $url}"

  private class ImageTexture(val location: ResourceLocation) extends AbstractTexture {
    var width = 0
    var height = 0

    override def loadTexture(manager: IResourceManager): Unit = {
      deleteGlTexture()

      var is: InputStream = null
      try {
        val resource = manager.getResource(location)
        is = resource.getInputStream
        val bi = ImageIO.read(is)
        TextureUtil.uploadTextureImageAllocate(getGlTextureId, bi, false, false)
        width = bi.getWidth
        height = bi.getHeight
      }
      finally {
        Option(is).foreach(_.close())
      }
    }
  }

}
