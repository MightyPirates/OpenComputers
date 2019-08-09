package li.cil.oc.client.renderer.font

import java.nio.ByteBuffer

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.FontTextureProvider.Receiver
import li.cil.oc.client.renderer.font.SingleGlyphFontTextureProvider.{CharIcon, CharTexture}
import li.cil.oc.util.{FontUtils, RenderState}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.{IReloadableResourceManager, IResourceManager, IResourceManagerReloadListener}
import net.minecraft.util.math.MathHelper
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

class SingleGlyphFontTextureProvider(private val glyphProvider: IGlyphProvider, private val dynamic: Boolean) extends FontTextureProvider with IResourceManagerReloadListener {
  private var texture: CharTexture = _

  private val charMap = new Int2ObjectOpenHashMap[CharIcon]

  private var activeTexture: CharTexture = _

  initialize()

  Minecraft.getMinecraft.getResourceManager match {
    case reloadable: IReloadableResourceManager => reloadable.registerReloadListener(this)
    case _ =>
  }

  def initialize() {
    if (texture != null) {
      texture.delete()
    }

    // estimate texture size required
    // be lazy about it, because most GPUs are way overkill for this... but it is 16MB
    val maxTextureSize = Minecraft.getGLMaximumTextureSize
    val worstCaseSideSize = 4096 // hardcoded for now... TODO

    if (maxTextureSize < worstCaseSideSize) {
      throw new CouldNotFitTextureException()
    }

    charMap.clear()
    texture = new CharTexture(worstCaseSideSize, worstCaseSideSize, this)

    if (dynamic) {
      // preload only basic chars
      StaticTextureFontTextureProvider.basicChars.foreach(c => getCodePoint(c))
    } else {
      for (i <- 0 until FontUtils.codepoint_limit) {
        getCodePoint(i)
      }
    }
  }

  def onResourceManagerReload(manager: IResourceManager) {
    glyphProvider.initialize()
    initialize()
  }

  private def charWidth = glyphProvider.getGlyphWidth

  private def charHeight = glyphProvider.getGlyphHeight

  override def getCharWidth: Int = charWidth

  override def getCharHeight: Int = charHeight

  override def getTextureCount: Int = 1

  override def begin(v: Int): Unit = {
    texture.bind()
  }

  override def end(v: Int): Unit = {

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
        receiver.draw(tx, tx + icon.w, ty, ty + icon.h, icon.u1, icon.u2, icon.v1, icon.v2)
      case null =>
    }
  }

  private def createCharIcon(char: Int): CharIcon = {
    if (FontUtils.wcwidth(char) < 1 || glyphProvider.getGlyph(char) == null) {
      if (char == '?') null
      else getCodePoint('?')
    }
    else {
      if (texture.isFull(char)) {
        throw new CouldNotFitTextureException()
      }

      texture.add(char)
    }
  }

  override def isDynamic: Boolean = dynamic

  override def loadCodePoint(codePoint: Int): Unit = if (dynamic) getCodePoint(codePoint)
}

object SingleGlyphFontTextureProvider {
  class CharTexture(val xSize: Int, val ySize: Int, val owner: SingleGlyphFontTextureProvider) {
    private val id = GlStateManager.generateTexture()
    RenderState.bindTexture(id)
    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    } else {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    }
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_INTENSITY8, xSize, ySize, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, null.asInstanceOf[ByteBuffer])
    RenderState.bindTexture(0)

    RenderState.checkError(getClass.getName + ".<init>: create texture")

    // Some padding to avoid bleeding.
    private val cellWidth = owner.charWidth + 2
    private val cellHeight = owner.charHeight + 2
    private val cols = xSize / cellWidth
    private val rows = ySize / cellHeight
    private val uStep = cellWidth / xSize.toFloat
    private val vStep = cellHeight / ySize.toFloat
    private val padU = 1.0f / xSize
    private val padV = 1.0f / ySize
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
      GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1 + x * cellWidth, 1 + y * cellHeight, w, h, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, owner.glyphProvider.getGlyph(char))

      chars += glyphWidth

      new CharIcon(w, h, padU + x * uStep, padV + y * vStep, (x + glyphWidth) * uStep - padU, (y + 1) * vStep - padV)
    }
  }

  class CharIcon(val w: Int, val h: Int, val u1: Float, val v1: Float, val u2: Float, val v2: Float) {

  }

}