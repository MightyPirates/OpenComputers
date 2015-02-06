package li.cil.oc.util

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import org.lwjgl.opengl._
import org.lwjgl.util.glu.GLU

object RenderState {
  val arb = GLContext.getCapabilities.GL_ARB_multitexture && !GLContext.getCapabilities.OpenGL13

  def checkError(where: String) {
    val error = GL11.glGetError
    if (error != 0 && Settings.get.logOpenGLErrors) {
      OpenComputers.log.warn("GL ERROR @ " + where + ": " + GLU.gluErrorString(error))
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
    Minecraft.getMinecraft.entityRenderer.disableLightmap()
    RenderHelper.disableStandardItemLighting()
  }

  def enableLighting() {
    Minecraft.getMinecraft.entityRenderer.enableLightmap()
    RenderHelper.enableStandardItemLighting()
  }

  def makeItBlend() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
  }

  def setBlendAlpha(alpha: Float) = {
    GL11.glColor4f(1, 1, 1, alpha)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
  }
}
