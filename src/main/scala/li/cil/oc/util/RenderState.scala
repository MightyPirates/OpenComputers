package li.cil.oc.util

import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.OpenGlHelper
import org.lwjgl.opengl._
import org.lwjgl.util.glu.GLU

object RenderState {
  val arb = GLContext.getCapabilities.GL_ARB_multitexture && !GLContext.getCapabilities.OpenGL13

  private val canUseBlendColor = GLContext.getCapabilities.OpenGL14

  def checkError(where: String) {
    val error = GL11.glGetError
    if (error != 0 && Settings.get.logOpenGLErrors) {
      OpenComputers.log.warning("GL ERROR @ " + where + ": " + GLU.gluErrorString(error))
    }
  }

  def compilingDisplayList = {
    if (GL11.glGetInteger(GL11.GL_LIST_INDEX) != 0) {
      val mode = GL11.glGetInteger(GL11.GL_LIST_MODE)
      mode == GL11.GL_COMPILE || mode == GL11.GL_COMPILE_AND_EXECUTE
    }
    else false
  }

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

  def enableLighting() {
    GL11.glEnable(GL11.GL_LIGHTING)
    if (arb) {
      ARBMultitexture.glActiveTextureARB(OpenGlHelper.lightmapTexUnit)
      GL11.glEnable(GL11.GL_TEXTURE_2D)
      ARBMultitexture.glActiveTextureARB(OpenGlHelper.defaultTexUnit)
    }
    else {
      GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit)
      GL11.glEnable(GL11.GL_TEXTURE_2D)
      GL13.glActiveTexture(OpenGlHelper.defaultTexUnit)
    }
  }

  def makeItBlend() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glDepthFunc(GL11.GL_LEQUAL)
  }

  def setBlendAlpha(alpha: Float) = if (canUseBlendColor) {
    GL14.glBlendColor(0, 0, 0, alpha)
    GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE)
  }
}
