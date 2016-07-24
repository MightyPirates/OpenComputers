package li.cil.oc.client.renderer.markdown.segment.render

import java.io.InputStream
import javax.imageio.ImageIO

import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class TextureImageRenderer(val location: ResourceLocation) extends ImageRenderer {
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

  override def getWidth: Int = texture.width

  override def getHeight: Int = texture.height

  override def render(mouseX: Int, mouseY: Int): Unit = {
    Textures.bind(location)
    GlStateManager.color(1, 1, 1, 1)
    GL11.glBegin(GL11.GL_QUADS)
    GL11.glTexCoord2f(0, 0)
    GL11.glVertex2f(0, 0)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex2f(0, texture.height)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex2f(texture.width, texture.height)
    GL11.glTexCoord2f(1, 0)
    GL11.glVertex2f(texture.width, 0)
    GL11.glEnd()
  }

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
