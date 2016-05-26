package li.cil.oc.util

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import org.lwjgl.opengl._
import org.lwjgl.util.glu.GLU

// This class has evolved into a wrapper for GlStateManager that basically does
// nothing but call the corresponding GlStateManager methods and then also
// forcefully applies whatever that call *should* do. This way the state
// manager's internal state is kept up-to-date but we also avoid issues with
// that state being incorrect causing wrong behavior (I've had too many render
// bugs where textures were not bound correctly or state was not updated
// because the state manager thought it already was in the state to change to,
// so I frankly don't care if this is less performant anymore).
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

  // pushAttrib/popAttrib currently breaks the GlStateManager because it doesn't
  // accordingly pushes/pops its cache, so it gets into an illegal state...
  // See https://gist.github.com/fnuecke/9a5b2499835fca9b52419277dc6239ca
  def pushAttrib(): Unit = {
//    GlStateManager.glPushAttrib(mask)
  }

  def popAttrib(): Unit = {
//    GlStateManager.popAttrib()
  }

  def disableEntityLighting() {
    Minecraft.getMinecraft.entityRenderer.disableLightmap()
    RenderHelper.disableStandardItemLighting()
  }

  def enableEntityLighting() {
    Minecraft.getMinecraft.entityRenderer.enableLightmap()
    RenderHelper.enableStandardItemLighting()
  }

  def makeItBlend() {
    GlStateManager.enableBlend()
    GL11.glEnable(GL11.GL_BLEND)
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
  }

  def setBlendAlpha(alpha: Float) = {
    GlStateManager.color(1, 1, 1, alpha)
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
  }

  def bindTexture(id: Int): Unit = {
    GlStateManager.bindTexture(id)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
  }
}
