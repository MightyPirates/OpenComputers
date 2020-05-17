package li.cil.oc.client.renderer.font

import com.google.common.base.Charsets
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.font.FontTextureProvider.Receiver
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

import scala.io.Source

/**
 * Font renderer using a user specified texture file, meaning the list of
 * supported characters is fixed. But at least this one works.
 */
class StaticTextureFontTextureProvider extends FontTextureProvider {
  protected val (chars, charWidth, charHeight) = try {
    val lines = Source.fromInputStream(Minecraft.getMinecraft.getResourceManager.getResource(new ResourceLocation(Settings.resourceDomain, "textures/font/chars.txt")).getInputStream)(Charsets.UTF_8).getLines()
    val chars = lines.next()
    val (w, h) = if (lines.hasNext) {
      val size = lines.next().split(" ", 2)
      (size(0).toInt, size(1).toInt)
    } else (10, 18)
    (chars, w, h)
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.warn("Failed reading font metadata, using defaults.", t)
      (StaticTextureFontTextureProvider.basicChars, 10, 18)
  }

  private val cols = 256.0f / charWidth
  private val uStep = charWidth / 256.0f
  private val uSize = uStep
  private val vStep = (charHeight + 1) / 256.0f
  private val vSize = charHeight / 256.0f
  private val s = Settings.get.fontCharScale
  private val dw = charWidth * s - charWidth
  private val dh = charHeight * s - charHeight

  override def getCharWidth: Int = charWidth

  override def getCharHeight: Int = charHeight

  private val charsMap = new Int2IntOpenHashMap()
  charsMap.defaultReturnValue(chars.indexOf('?'))

  for (idx <- 0 until chars.length) {
    charsMap.put(chars.codePointAt(idx), idx)
  }

  override def getTextureCount: Int = 1

  override def begin(texture: Int) {
    if (Settings.get.textAntiAlias) {
      Textures.bind(Textures.Font.AntiAliased)
    }
    else {
      Textures.bind(Textures.Font.Aliased)
    }
  }

  override def end(texture: Int) {

  }

  override def drawCodePoint(char: Int, tx: Float, ty: Float, receiver: Receiver) {
    val index = charsMap.get(char)
    if (index >= 0) {
      val x = index % cols
      val y = index / cols
      val u = x * uStep
      val v = y * vStep
      receiver.draw(tx - dw, tx + charWidth * s, ty - dh, ty + charHeight * s, u, u + uSize, v, v + vSize)
    }
  }

  override def isDynamic: Boolean = false

  override def loadCodePoint(codePoint: Int): Unit = {

  }
}

object StaticTextureFontTextureProvider {
  final val basicChars = """☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■"""
}