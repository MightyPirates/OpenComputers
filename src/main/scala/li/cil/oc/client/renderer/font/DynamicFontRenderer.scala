package li.cil.oc.client.renderer.font

import java.awt.Font
import java.io.InputStream

import li.cil.oc.client.renderer.font.DynamicFontRenderer.CharTexture
import li.cil.oc.util.{FontUtil, RenderState}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl._

import scala.collection.mutable

/**
 * Font renderer that dynamically generates lookup textures by rendering a font
 * to it. It's pretty broken right now, and font rendering looks crappy as hell.
 */
class DynamicFontRenderer(val charRenderer: DynamicCharRenderer) extends TextureFontRenderer {
  private val textures = mutable.ArrayBuffer(new DynamicFontRenderer.CharTexture(this))

  private val charMap = mutable.Map.empty[Char, DynamicFontRenderer.CharIcon]

  private val fbo = GL30.glGenFramebuffers()

  private val rbo = GL30.glGenRenderbuffers()

  GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
  GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo)

  GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, charWidth * 2, charHeight)
  GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, rbo)

  GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0)
  GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

  var activeTexture: CharTexture = textures(0)

  generateChars(basicChars.toCharArray)

  RenderState.checkError(getClass.getName + ".<init>: glGenFramebuffers")

  override protected def charWidth = charRenderer.charWidth.toInt

  override protected def charHeight = charRenderer.charHeight.toInt

  override protected def textureCount = textures.length

  override protected def bindTexture(index: Int) {
    activeTexture = textures(index)
    activeTexture.bind()
    RenderState.checkError(getClass.getName + ".bindTexture")
  }

  override protected def generateChar(char: Char) {
    charMap.getOrElseUpdate(char, createCharIcon(char))
  }

  override protected def drawChar(tx: Float, ty: Float, char: Char) {
    val icon = charMap(char)
    if (icon.texture == activeTexture) {
      icon.draw(tx, ty)
    }
  }

  private def createCharIcon(char: Char): DynamicFontRenderer.CharIcon = {
    if (!charRenderer.canDisplay(char)) {
      if (char == '?') null
      else charMap.getOrElseUpdate('?', createCharIcon('?'))
    }
    else {
      if (textures.last.isFull) {
        textures += new DynamicFontRenderer.CharTexture(this)
        textures.last.bind()
      }
      textures.last.add(char)
    }
  }
}

object DynamicFontRenderer {
  private val size = 256

  class CharTexture(val owner: DynamicFontRenderer) {
    private val id = GL11.glGenTextures()
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, BufferUtils.createByteBuffer(size * size * 4))

    RenderState.checkError(getClass.getName + ".<init>: create texture")

    // Some padding to avoid bleeding.
    private val cellWidth = owner.charWidth + 2
    private val cellHeight = owner.charHeight + 2
    private val cols = size / cellWidth
    private val rows = size / cellHeight
    private val uStep = cellWidth / size.toFloat
    private val vStep = cellHeight / size.toFloat
    private val pad = 1.0 / size
    private val capacity = cols * rows

    private var chars = 0

    def bind() {
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
    }

    def isFull = chars >= capacity

    def add(char: Char) = {
      // TODO force to next row if wide char and won't fit into row.
      val x = chars % cols
      val y = chars / cols
      val w = owner.charWidth * FontUtil.wcwidth(char)
      val h = owner.charHeight

      GL11.glDisable(GL11.GL_DEPTH_TEST)
      GL11.glDepthMask(false)

      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, owner.fbo)
      GL11.glClearColor(0, 0, 0, 0)
      GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0)
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

      GL11.glViewport(0, 0, owner.charWidth, h)

      GL11.glMatrixMode(GL11.GL_PROJECTION)
      GL11.glPushMatrix()
      GL11.glLoadIdentity()

      GL11.glOrtho(0, w, h, 0, 0, 1)

      GL11.glMatrixMode(GL11.GL_MODELVIEW)
      GL11.glPushMatrix()
      GL11.glLoadIdentity()
      GL11.glTranslatef(0, 0, -0.5f)

      owner.charRenderer.drawChar(char)

      GL43.glCopyImageSubData(owner.rbo, GL30.GL_RENDERBUFFER, 0, 0, 0, 0, id, GL11.GL_TEXTURE_2D, 0, 1 + x * cellWidth, 1 + y * cellHeight, 0, w, h, 1)

      GL11.glMatrixMode(GL11.GL_PROJECTION)
      GL11.glPopMatrix()
      GL11.glMatrixMode(GL11.GL_MODELVIEW)
      GL11.glPopMatrix()

      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

      chars += FontUtil.wcwidth(char)

      new CharIcon(this, w, h, pad + x * uStep, pad + y * vStep, (x + 1) * uStep - 2 * pad, (y + 1) * vStep - 2 * pad)
    }
  }

  class CharIcon(val texture: CharTexture, val w: Int, val h: Int, val u1: Double, val v1: Double, val u2: Double, val v2: Double) {
    def draw(tx: Float, ty: Float) {
      GL11.glTexCoord2d(u1, v1)
      GL11.glVertex2f(tx, ty + h)
      GL11.glTexCoord2d(u2, v1)
      GL11.glVertex2f(tx + w, ty + h)
      GL11.glTexCoord2d(u2, v2)
      GL11.glVertex2f(tx + w, ty)
      GL11.glTexCoord2d(u1, v2)
      GL11.glVertex2f(tx, ty)
    }
  }

}