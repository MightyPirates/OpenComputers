package li.cil.oc.util

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.GlStateManager.CullFace
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

  def pushAttrib(mask: Int = 8256): Unit = {
    GL11.glPushAttrib(mask)
  }

  def popAttrib(): Unit = {
    GlStateManager.popAttrib()
  }

  def pushMatrix(): Unit = {
    GlStateManager.pushMatrix()
  }

  def popMatrix(): Unit = {
    GlStateManager.popMatrix()
  }

  def color(r: Float, g: Float, b: Float, a: Float = 1f): Unit = {
    GlStateManager.color(r, g, b, a)
    GL11.glColor4f(r, g, b, a)
  }

  def disableColorMask(): Unit = {
    GlStateManager.colorMask(false, false, false, false)
    GL11.glColorMask(false, false, false, false)
  }

  def enableColorMask(): Unit = {
    GlStateManager.colorMask(true, true, true, true)
    GL11.glColorMask(true, true, true, true)
  }

  def disableCullFace(): Unit = {
    GlStateManager.disableCull()
    GL11.glDisable(GL11.GL_CULL_FACE)
  }

  def enableCullFace(): Unit = {
    GlStateManager.enableCull()
    GL11.glEnable(GL11.GL_CULL_FACE)
  }

  def disableDepth(): Unit = {
    GlStateManager.disableDepth()
    GL11.glDisable(GL11.GL_DEPTH_TEST)
  }

  def enableDepth(): Unit = {
    GlStateManager.enableDepth()
    GL11.glEnable(GL11.GL_DEPTH_TEST)
  }

  def disableDepthMask(): Unit = {
    GlStateManager.depthMask(false)
    GL11.glDepthMask(false)
  }

  def enableDepthMask(): Unit = {
    GlStateManager.depthMask(true)
    GL11.glDepthMask(true)
  }

  def disableLighting(): Unit = {
    GlStateManager.disableLighting()
    GL11.glDisable(GL11.GL_LIGHTING)
  }

  def enableLighting(): Unit = {
    GlStateManager.enableLighting()
    GL11.glEnable(GL11.GL_LIGHTING)
  }

  def disableEntityLighting() {
    Minecraft.getMinecraft.entityRenderer.disableLightmap()
    RenderHelper.disableStandardItemLighting()
  }

  def enableEntityLighting() {
    Minecraft.getMinecraft.entityRenderer.enableLightmap()
    RenderHelper.enableStandardItemLighting()
  }

  def disableRescaleNormal(): Unit = {
    GlStateManager.disableRescaleNormal()
    GL11.glDisable(GL12.GL_RESCALE_NORMAL)
  }

  def enableRescaleNormal(): Unit = {
    GlStateManager.enableRescaleNormal()
    GL11.glEnable(GL12.GL_RESCALE_NORMAL)
  }

  def makeItBlend() {
    GlStateManager.enableBlend()
    GL11.glEnable(GL11.GL_BLEND)
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
  }

  def disableBlend(): Unit = {
    GlStateManager.disableBlend()
    GL11.glDisable(GL11.GL_BLEND)
  }

  def blendFunc(sFactor: Int, dFactor: Int): Unit = {
    GlStateManager.blendFunc(sFactor, dFactor)
    GL11.glBlendFunc(sFactor, dFactor)
  }

  def cullFace(mode: Int): Unit = {
    GlStateManager.cullFace(mode match {
      case GL11.GL_BACK => CullFace.BACK
      case GL11.GL_FRONT => CullFace.FRONT
      case GL11.GL_FRONT_AND_BACK => CullFace.FRONT_AND_BACK
      case _ => CullFace.BACK // WTF?
    })
    GL11.glCullFace(mode)
  }

  def depthFunc(func: Int): Unit = {
    GlStateManager.depthFunc(func)
    GL11.glDepthFunc(func)
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
