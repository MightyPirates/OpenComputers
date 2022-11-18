package li.cil.oc.client.renderer.markdown.segment.render

import java.io.InputStream
import javax.imageio.ImageIO

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.Texture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.util.math.vector.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil

class TextureImageRenderer(val location: ResourceLocation) extends ImageRenderer {
  private val texture = {
    val manager = Minecraft.getInstance.getTextureManager
    manager.getTexture(location) match {
      case image: ImageTexture => image
      case other =>
        if (other != null) other.releaseId()
        val image = new ImageTexture(location)
        manager.register(location, image)
        image
    }
  }

  override def getWidth: Int = texture.width

  override def getHeight: Int = texture.height

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int): Unit = {
    Textures.bind(location)
    RenderSystem.color4f(1, 1, 1, 1)
    GL11.glBegin(GL11.GL_QUADS)
    GL11.glTexCoord2f(0, 0)
    val matrix = stack.last.pose
    val vec = new Vector4f(0, 0, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glTexCoord2f(0, 1)
    vec.set(0, texture.height, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glTexCoord2f(1, 1)
    vec.set(texture.width, texture.height, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glTexCoord2f(1, 0)
    vec.set(texture.width, 0, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glEnd()
  }

  private class ImageTexture(val location: ResourceLocation) extends Texture {
    var width = 0
    var height = 0

    override def load(manager: IResourceManager): Unit = {
      releaseId()

      var is: InputStream = null
      try {
        val resource = manager.getResource(location)
        is = resource.getInputStream
        val bi = ImageIO.read(is)
        val data = MemoryUtil.memAllocInt(bi.getWidth * bi.getHeight)
        val tempArr = Array.ofDim[Int]((1024 * 1024) min data.capacity)
        val dy = tempArr.length / bi.getWidth
        for (y0 <- 0 until bi.getHeight by dy) {
          val currH = dy min (bi.getHeight - y0 - 1)
          bi.getRGB(0, y0, bi.getWidth, currH, tempArr, 0, bi.getWidth)
          data.put(tempArr, 0, bi.getWidth * currH)
        }

        bind()
        data.flip()
        TextureUtil.initTexture(data, bi.getWidth, bi.getHeight)
        width = bi.getWidth
        height = bi.getHeight
      }
      finally {
        Option(is).foreach(_.close())
      }
    }
  }

}
