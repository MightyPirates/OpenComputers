package li.cil.oc.client

import net.minecraft.client.renderer.OpenGlHelper
import org.lwjgl.opengl.{ARBMultitexture, GLContext, GL11, GL13}

object RenderUtil {
  val arb = GLContext.getCapabilities.GL_ARB_multitexture && !GLContext.getCapabilities.OpenGL13

  def disableLighting() {
    GL11.glDisable(GL11.GL_LIGHTING)
    if (arb) {
      ARBMultitexture.glActiveTextureARB(OpenGlHelper.lightmapTexUnit)
      GL11.glDisable(GL11.GL_TEXTURE_2D)
      ARBMultitexture.glActiveTextureARB(OpenGlHelper.defaultTexUnit)
    }
    else {
      GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit)
      GL11.glDisable(GL11.GL_TEXTURE_2D)
      GL13.glActiveTexture(OpenGlHelper.defaultTexUnit)
    }
  }

  def makeItBlend() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR)
    GL11.glDepthFunc(GL11.GL_LEQUAL)
  }
}
