package li.cil.oc.client.renderer.font

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.Settings
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.client.renderer.font.DynamicFontRenderer.CharTexture
import li.cil.oc.util.FontUtils
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.resources.IReloadableResourceManager
import net.minecraft.resources.IResourceManager
import net.minecraft.resources.IResourceManagerReloadListener
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.util.math.vector.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl._

import scala.collection.mutable

/**
 * Font renderer that dynamically generates lookup textures by rendering a font
 * to it. It's pretty broken right now, and font rendering looks crappy as hell.
 */
class DynamicFontRenderer extends TextureFontRenderer with IResourceManagerReloadListener {
  private val glyphProvider: IGlyphProvider = Settings.get.fontRenderer match {
    case _ => new FontParserHex()
  }

  private val textures = mutable.ArrayBuffer.empty[CharTexture]

  private val charMap = mutable.Map.empty[Char, DynamicFontRenderer.CharIcon]

  private var activeTexture: CharTexture = _

  initialize()

  Minecraft.getInstance.getResourceManager match {
    case reloadable: IReloadableResourceManager => reloadable.registerReloadListener(this)
    case _ =>
  }

  def initialize() {
    for (texture <- textures) {
      texture.delete()
    }
    textures.clear()
    charMap.clear()
    glyphProvider.initialize()
    textures += new DynamicFontRenderer.CharTexture(this)
    activeTexture = textures.head
    generateChars(basicChars.toCharArray)
  }

  def onResourceManagerReload(manager: IResourceManager) {
    initialize()
  }

  override protected def charWidth = glyphProvider.getGlyphWidth

  override protected def charHeight = glyphProvider.getGlyphHeight

  override protected def textureCount = textures.length

  override protected def bindTexture(index: Int) {
    activeTexture = textures(index)
    activeTexture.bind()
    RenderState.checkError(getClass.getName + ".bindTexture")
  }

  override protected def selectType(index: Int): RenderType = {
    activeTexture = textures(index)
    activeTexture.getType
  }

  override protected def generateChar(char: Char) {
    charMap.getOrElseUpdate(char, createCharIcon(char))
  }

  override protected def drawChar(matrix: Matrix4f, tx: Float, ty: Float, char: Char) {
    charMap.get(char) match {
      case Some(icon) if icon.texture == activeTexture => icon.draw(matrix, tx, ty)
      case _ =>
    }
  }

  override protected def drawChar(builder: IVertexBuilder, matrix: Matrix4f, color: Int, tx: Float, ty: Float, char: Char) {
    charMap.get(char) match {
      case Some(icon) if icon.texture == activeTexture => icon.draw(builder, matrix, color, tx, ty)
      case _ =>
    }
  }

  private def createCharIcon(char: Char): DynamicFontRenderer.CharIcon = {
    if (FontUtils.wcwidth(char) < 1 || glyphProvider.getGlyph(char) == null) {
      if (char == '?') null
      else charMap.getOrElseUpdate('?', createCharIcon('?'))
    }
    else {
      if (textures.last.isFull(char)) {
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
    private val id = TextureUtil.generateTextureId()
    RenderState.bindTexture(id)
    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    } else {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    }
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, size, size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, BufferUtils.createByteBuffer(size * size * 4))
    RenderState.bindTexture(0)

    private val rt = RenderTypes.createFontTex(id)

    RenderState.checkError(getClass.getName + ".<init>: create texture")

    // Some padding to avoid bleeding.
    private val cellWidth = owner.charWidth + 2
    private val cellHeight = owner.charHeight + 2
    private val cols = size / cellWidth
    private val rows = size / cellHeight
    private val uStep = cellWidth / size.toFloat
    private val vStep = cellHeight / size.toFloat
    private val pad = 1f / size
    private val capacity = cols * rows

    private var chars = 0

    def delete() {
      RenderSystem.deleteTexture(id)
    }

    def bind() {
      RenderState.bindTexture(id)
    }

    def getType() = rt

    def isFull(char: Char) = chars + FontUtils.wcwidth(char) > capacity

    def add(char: Char) = {
      val glyphWidth = FontUtils.wcwidth(char)
      val w = owner.charWidth * glyphWidth
      val h = owner.charHeight
      // Force line break if we have a char that's wider than what space remains in this row.
      if (chars % cols + glyphWidth > cols) {
        chars += 1
      }
      val x = chars % cols
      val y = chars / cols

      RenderState.bindTexture(id)
      GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1 + x * cellWidth, 1 + y * cellHeight, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, owner.glyphProvider.getGlyph(char))

      chars += glyphWidth

      new CharIcon(this, w, h, pad + x * uStep, pad + y * vStep, (x + glyphWidth) * uStep - pad, (y + 1) * vStep - pad)
    }
  }

  class CharIcon(val texture: CharTexture, val w: Int, val h: Int, val u1: Float, val v1: Float, val u2: Float, val v2: Float) {
    def draw(matrix: Matrix4f, tx: Float, ty: Float) {
      GL11.glTexCoord2f(u1, v2)
      val vec = new Vector4f(tx, ty + h, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      GL11.glTexCoord2f(u2, v2)
      vec.set(tx + w, ty + h, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      GL11.glTexCoord2f(u2, v1)
      vec.set(tx + w, ty, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
      GL11.glTexCoord2f(u1, v1)
      vec.set(tx, ty, 0, 1)
      vec.transform(matrix)
      GL11.glVertex3f(vec.x, vec.y, vec.z)
    }

    def draw(builder: IVertexBuilder, matrix: Matrix4f, color: Int, tx: Float, ty: Float) {
      val r = ((color >> 16) & 0xFF) / 255f
      val g = ((color >> 8) & 0xFF) / 255f
      val b = (color & 0xFF) / 255f
      builder.vertex(matrix, tx, ty + h, 0).color(r, g, b, 1f).uv(u1, v2).endVertex()
      builder.vertex(matrix, tx + w, ty + h, 0).color(r, g, b, 1f).uv(u2, v2).endVertex()
      builder.vertex(matrix, tx + w, ty, 0).color(r, g, b, 1f).uv(u2, v1).endVertex()
      builder.vertex(matrix, tx, ty, 0).color(r, g, b, 1f).uv(u1, v1).endVertex()
    }
  }

}