package li.cil.oc.client.renderer.font

import com.google.common.base.Charsets
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.util.math.vector.Vector4f
import org.lwjgl.opengl.GL11

import scala.io.Source

/**
 * Font renderer using a user specified texture file, meaning the list of
 * supported characters is fixed. But at least this one works.
 */
class StaticFontRenderer extends TextureFontRenderer {
  protected val (chars, charWidth, charHeight) = try {
    val lines = Source.fromInputStream(Minecraft.getInstance.getResourceManager.getResource(new ResourceLocation(Settings.resourceDomain, "textures/font/chars.txt")).getInputStream)(Charsets.UTF_8).getLines()
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
      (basicChars, 10, 18)
  }

  private val cols = 256 / charWidth
  private val uStep = charWidth / 256f
  private val uSize = uStep
  private val vStep = (charHeight + 1) / 256f
  private val vSize = charHeight / 256f
  private val s = Settings.get.fontCharScale.toFloat
  private val dw = charWidth * s - charWidth
  private val dh = charHeight * s - charHeight

  override protected def textureCount = 1

  override protected def bindTexture(index: Int) {
    if (Settings.get.textAntiAlias) {
      Textures.bind(Textures.Font.AntiAliased)
    }
    else {
      Textures.bind(Textures.Font.Aliased)
    }
    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    }
    else {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    }
  }

  override protected def selectType(index: Int): RenderType = {
    if (Settings.get.textAntiAlias) {
      RenderTypes.createFontTex("smoothed", Textures.Font.AntiAliased, Settings.get.textLinearFiltering)
    }
    else {
      RenderTypes.createFontTex("aliased", Textures.Font.Aliased, Settings.get.textLinearFiltering)
    }
  }

  override protected def drawChar(matrix: Matrix4f, tx: Float, ty: Float, char: Char) {
    val index = 1 + (chars.indexOf(char) match {
      case -1 => chars.indexOf('?')
      case i => i
    })
    val x = (index - 1) % cols
    val y = (index - 1) / cols
    val u = x * uStep
    val v = y * vStep
    GL11.glTexCoord2d(u, v + vSize)
    val vec = new Vector4f(tx - dw, ty + charHeight * s, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glTexCoord2d(u + uSize, v + vSize)
    vec.set(tx + charWidth * s, ty + charHeight * s, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glTexCoord2d(u + uSize, v)
    vec.set(tx + charWidth * s, ty - dh, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
    GL11.glTexCoord2d(u, v)
    vec.set(tx - dw, ty - dh, 0, 1)
    vec.transform(matrix)
    GL11.glVertex3f(vec.x, vec.y, vec.z)
  }

  protected def drawChar(builder: IVertexBuilder, matrix: Matrix4f, color: Int, tx: Float, ty: Float, char: Char) {
    val index = 1 + (chars.indexOf(char) match {
      case -1 => chars.indexOf('?')
      case i => i
    })
    val x = (index - 1) % cols
    val y = (index - 1) / cols
    val u = x * uStep
    val v = y * vStep
    val r = ((color >> 16) & 0xFF) / 255f
    val g = ((color >> 8) & 0xFF) / 255f
    val b = (color & 0xFF) / 255f
    builder.vertex(matrix, tx - dw, ty + charHeight * s, 0).color(r, g, b, 1f).uv(u, v + vSize).endVertex()
    builder.vertex(matrix, tx + charWidth * s, ty + charHeight * s, 0).color(r, g, b, 1f).uv(u + uSize, v + vSize).endVertex()
    builder.vertex(matrix, tx + charWidth * s, ty - dh, 0).color(r, g, b, 1f).uv(u + uSize, v).endVertex()
    builder.vertex(matrix, tx - dw, ty - dh, 0).color(r, g, b, 1f).uv(u, v).endVertex()
  }

  override protected def generateChar(char: Char) {}
}
