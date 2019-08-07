package li.cil.oc.client.renderer.font

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.MultiDynamicFontTextureProvider.{CharIcon, CharTexture}
import li.cil.oc.client.renderer.font.FontTextureProvider.Receiver
import li.cil.oc.util.FontUtils
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.IReloadableResourceManager
import net.minecraft.client.resources.IResourceManager
import net.minecraft.client.resources.IResourceManagerReloadListener
import org.lwjgl.BufferUtils
import org.lwjgl.opengl._

import scala.collection.mutable

/**
 * Font renderer that dynamically generates lookup textures by rendering a font
 * to it. It's pretty broken right now, and font rendering looks crappy as hell.
 */
class MultiDynamicFontTextureProvider(private val glyphProvider: IGlyphProvider) extends FontTextureProvider with IResourceManagerReloadListener {
  private val textures = mutable.ArrayBuffer.empty[CharTexture]

  private val charMap = new Int2ObjectOpenHashMap[MultiDynamicFontTextureProvider.CharIcon]

  private var activeTexture: CharTexture = _

  initialize()

  Minecraft.getMinecraft.getResourceManager match {
    case reloadable: IReloadableResourceManager => reloadable.registerReloadListener(this)
    case _ =>
  }

  def initialize() {
    for (texture <- textures) {
      texture.delete()
    }

    textures.clear()
    charMap.clear()
    textures += new MultiDynamicFontTextureProvider.CharTexture(this)
    activeTexture = textures.head

    StaticFontTextureProvider.basicChars.foreach(c => getCodePoint(c))
  }

  def onResourceManagerReload(manager: IResourceManager) {
    glyphProvider.initialize()
    initialize()
  }

  private def charWidth = glyphProvider.getGlyphWidth

  private def charHeight = glyphProvider.getGlyphHeight

  override def getCharWidth: Int = charWidth

  override def getCharHeight: Int = charHeight

  override def getTextureCount: Int = textures.size

  override def begin(tex: Int): Unit = {
    activeTexture = textures(tex)
    activeTexture.bind()
    RenderState.checkError(getClass.getName + ".begin")
  }

  override def end(tex: Int): Unit = {

  }

  private def getCodePoint(char: Int): CharIcon = {
    charMap.get(char) match {
      case icon: CharIcon => icon
      case null =>
        val result = createCharIcon(char)
        charMap.put(char, result)
        result
    }
  }

  override def drawCodePoint(char: Int, tx: Float, ty: Float, receiver: Receiver) {
    getCodePoint(char) match {
      case icon: CharIcon =>
        if (icon.texture == activeTexture) {
          receiver.draw(tx, tx + icon.w, ty, ty + icon.h, icon.u1, icon.u2, icon.v1, icon.v2)
        }
      case null =>
    }
  }

  private def createCharIcon(char: Int): MultiDynamicFontTextureProvider.CharIcon = {
    if (FontUtils.wcwidth(char) < 1 || glyphProvider.getGlyph(char) == null) {
      if (char == '?') null
      else getCodePoint('?')
    }
    else {
      if (textures.last.isFull(char)) {
        textures += new MultiDynamicFontTextureProvider.CharTexture(this)
        textures.last.bind()
      }
      textures.last.add(char)
    }
  }
}

object MultiDynamicFontTextureProvider {
  private val size = 512

  class CharTexture(val owner: MultiDynamicFontTextureProvider) {
    private val id = GlStateManager.generateTexture()
    RenderState.bindTexture(id)
    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    } else {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    }
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA8, size, size, 0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, BufferUtils.createByteBuffer(size * size * 4))
    RenderState.bindTexture(0)

    RenderState.checkError(getClass.getName + ".<init>: create texture")

    // Some padding to avoid bleeding.
    private val cellWidth = owner.charWidth + 2
    private val cellHeight = owner.charHeight + 2
    private val cols = size / cellWidth
    private val rows = size / cellHeight
    private val uStep = cellWidth / size.toFloat
    private val vStep = cellHeight / size.toFloat
    private val pad = 1.0f / size
    private val capacity = cols * rows

    private var chars = 0

    def delete() {
      GlStateManager.deleteTexture(id)
    }

    def bind() {
      RenderState.bindTexture(id)
    }

    def isFull(char: Int): Boolean = chars + FontUtils.wcwidth(char) > capacity

    def add(char: Int): CharIcon = {
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
      GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1 + x * cellWidth, 1 + y * cellHeight, w, h, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, owner.glyphProvider.getGlyph(char))

      chars += glyphWidth

      new CharIcon(this, w, h, pad + x * uStep, pad + y * vStep, (x + glyphWidth) * uStep - pad, (y + 1) * vStep - pad)
    }
  }

  class CharIcon(val texture: CharTexture, val w: Int, val h: Int, val u1: Float, val v1: Float, val u2: Float, val v2: Float) {

  }

}