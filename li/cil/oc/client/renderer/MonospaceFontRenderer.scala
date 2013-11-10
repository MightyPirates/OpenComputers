package li.cil.oc.client.renderer

import li.cil.oc.util.{RenderState, PackedColor}
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import scala.io.Source

object MonospaceFontRenderer {
  private val font = new ResourceLocation(Config.resourceDomain, "textures/font/chars.png")

  private val chars = Source.fromInputStream(MonospaceFontRenderer.getClass.getResourceAsStream("/assets/" + Config.resourceDomain + "/textures/font/chars.txt")).mkString

  private var instance: Option[Renderer] = None

  def init(textureManager: TextureManager) = this.synchronized(
    instance = instance.orElse(Some(new Renderer(textureManager))))

  val (fontWidth, fontHeight) = (5, 9)

  def drawString(x: Int, y: Int, value: Array[Char], color: Array[Int], depth: PackedColor.Depth.Value) = instance match {
    case None => OpenComputers.log.warning("Trying to render string with uninitialized MonospaceFontRenderer.")
    case Some(renderer) => renderer.drawString(x, y, value, color, depth)
  }

  private class Renderer(private val textureManager: TextureManager) {
    /** Display lists, one per char (renders quad with char's uv coords). */
    private val charLists = GLAllocation.generateDisplayLists(256)

    /** Buffer filled with char display lists to efficiently draw strings. */
    private val listBuffer = GLAllocation.createDirectIntBuffer(512)

    private val (charWidth, charHeight) = (MonospaceFontRenderer.fontWidth * 2, MonospaceFontRenderer.fontHeight * 2)
    private val cols = 256 / charWidth
    private val uStep = charWidth / 256.0
    private val vStep = charHeight / 256.0

    // Set up the display lists.
    {
      val t = Tessellator.instance
      // Now create lists for all printable chars.
      for (index <- 1 until 0xFF) {
        val x = (index - 1) % cols
        val y = (index - 1) / cols
        val u = x * uStep
        val v = y * vStep
        GL11.glNewList(charLists + index, GL11.GL_COMPILE)
        t.startDrawingQuads()
        t.addVertexWithUV(0, charHeight, 0, u, v + vStep)
        t.addVertexWithUV(charWidth, charHeight, 0, u + uStep, v + vStep)
        t.addVertexWithUV(charWidth, 0, 0, u + uStep, v)
        t.addVertexWithUV(0, 0, 0, u, v)
        t.draw()
        GL11.glTranslatef(charWidth, 0, 0)
        GL11.glEndList()
      }
      // Special case for whitespace: just translate, don't render.
      GL11.glNewList(charLists + ' ', GL11.GL_COMPILE)
      GL11.glTranslatef(charWidth, 0, 0)
      GL11.glEndList()
    }

    def drawString(x: Int, y: Int, value: Array[Char], color: Array[Int], depth: PackedColor.Depth.Value) = {
      if (color.length != value.length) throw new IllegalArgumentException("Color count must match char count.")

      textureManager.bindTexture(MonospaceFontRenderer.font)
      GL11.glPushMatrix()
      GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
      GL11.glTranslatef(x, y, 0)
      GL11.glScalef(0.5f, 0.5f, 1)
      GL11.glDepthMask(false)
      RenderState.makeItBlend()

      // Background first. We try to merge adjacent backgrounds of the same
      // color to reduce the number of quads we have to draw.
      var cbg = 0x000000
      var width = 0
      for (col <- color.map(PackedColor.unpackBackground(_, depth))) {
        if (col != cbg) {
          draw(cbg, width)
          cbg = col
          width = 0
        }
        width = width + 1
      }
      draw(cbg, width)

      // Foreground second. We only have to flush when the color changes, so
      // unless every char has a different color this should be quite efficient.
      var cfg = 0x000000
      GL11.glColor3f(0, 0, 0)
      for ((ch, col) <- value.zip(color.map(PackedColor.unpackForeground(_, depth)))) {
        val index = 1 + chars.indexOf(ch) match {
          case -1 => chars.indexOf('?')
          case i => i
        }
        if (col != cfg) {
          // Color changed, force flush and adjust colors.
          flush()
          cfg = col
          GL11.glColor3ub(
            (cfg & 0xFF0000 >> 16).toByte,
            (cfg & 0x00FF00 >> 8).toByte,
            (cfg & 0x0000FF).toByte)
        }
        listBuffer.put(charLists + index)
        if (listBuffer.remaining == 0)
          flush()
      }
      flush()

      GL11.glPopAttrib()
      GL11.glPopMatrix()
    }

    private def draw(color: Int, width: Int) = if (color != 0 && width > 0) {
      val t = Tessellator.instance
      t.startDrawingQuads()
      t.setColorOpaque_I(color)
      t.addVertexWithUV(0, charHeight, 0, 0, vStep)
      t.addVertexWithUV(charWidth, charHeight, 0, width * uStep, vStep)
      t.addVertexWithUV(charWidth, 0, 0, width * uStep, 0)
      t.addVertexWithUV(0, 0, 0, 0, 0)
      t.draw()
    }

    private def flush() = {
      listBuffer.flip()
      GL11.glCallLists(listBuffer)
      listBuffer.clear()
    }
  }

}