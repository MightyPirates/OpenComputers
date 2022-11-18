package li.cil.oc.util

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import org.lwjgl.opengl._

// This class has evolved into a wrapper for RenderSystem that basically does
// nothing but call the corresponding RenderSystem methods and then also
// forcefully applies whatever that call *should* do. This way the state
// manager's internal state is kept up-to-date but we also avoid issues with
// that state being incorrect causing wrong behavior (I've had too many render
// bugs where textures were not bound correctly or state was not updated
// because the state manager thought it already was in the state to change to,
// so I frankly don't care if this is less performant anymore).
object RenderState {
  def getErrorString(errorCode: Int): String = errorCode match {
    case GL11.GL_NO_ERROR => "No error"
    case GL11.GL_INVALID_ENUM => "Enum argument out of range"
    case GL11.GL_INVALID_VALUE => "Numeric argument out of range"
    case GL11.GL_INVALID_OPERATION => "Operation illegal in current state"
    case GL11.GL_STACK_OVERFLOW => "Command would cause a stack overflow"
    case GL11.GL_STACK_UNDERFLOW => "Command would cause a stack underflow"
    case GL11.GL_OUT_OF_MEMORY => "Not enough memory left to execute command"
    case _ => f"Unknown [0x$errorCode%X]"
  }

  def checkError(where: String) {
    val error = GL11.glGetError
    if (error != 0 && Settings.get.logOpenGLErrors) {
      OpenComputers.log.warn("GL ERROR @ " + where + ": " + getErrorString(error))
    }
  }

  def compilingDisplayList = {
    if (GL11.glGetInteger(GL11.GL_LIST_INDEX) != 0) {
      val mode = GL11.glGetInteger(GL11.GL_LIST_MODE)
      mode == GL11.GL_COMPILE || mode == GL11.GL_COMPILE_AND_EXECUTE
    }
    else false
  }

  // pushAttrib/popAttrib currently breaks the RenderSystem because it doesn't
  // accordingly pushes/pops its cache, so it gets into an illegal state...
  // See https://gist.github.com/fnuecke/9a5b2499835fca9b52419277dc6239ca
  def pushAttrib(): Unit = {
//    RenderSystem.glPushAttrib(mask)
  }

  def popAttrib(): Unit = {
//    RenderSystem.popAttrib()
  }

  def disableEntityLighting() {
    RenderSystem.disableLighting()
    RenderSystem.disableColorMaterial()
  }

  def enableEntityLighting() {
    RenderSystem.enableLighting()
    RenderSystem.enableColorMaterial()
  }

  def makeItBlend() {
    RenderSystem.enableBlend()
    GL11.glEnable(GL11.GL_BLEND)
    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
  }

  def disableBlend() {
    RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ZERO)
    RenderSystem.disableBlend()
    GL11.glDisable(GL11.GL_BLEND)
  }

  def setBlendAlpha(alpha: Float) = {
    RenderSystem.color4f(1, 1, 1, alpha)
    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
  }

  def bindTexture(id: Int): Unit = {
    RenderSystem.bindTexture(id)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
  }
}
