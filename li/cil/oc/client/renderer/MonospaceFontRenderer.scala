package li.cil.oc.client.renderer

import li.cil.oc.util.{RenderState, PackedColor}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import scala.io.Source

object MonospaceFontRenderer {
  val font = new ResourceLocation(Settings.resourceDomain, "textures/font/chars.png")
  val fontAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars_aliased.png")

  private val chars = Source.fromInputStream(MonospaceFontRenderer.getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/textures/font/chars.txt"))("UTF-8").mkString

  private var instance: Option[Renderer] = None

  def init(textureManager: TextureManager) = this.synchronized(
    instance = instance.orElse(Some(new Renderer(textureManager))))

  val fontWidth = 5
  val fontHeight = 9

  def drawString(x: Int, y: Int, value: Array[Char], color: Array[Short], depth: PackedColor.Depth.Value) = instance match {
    case None => OpenComputers.log.warning("Trying to render string with uninitialized MonospaceFontRenderer.")
    case Some(renderer) => renderer.drawString(x, y, value, color, depth)
  }

  private class Renderer(private val textureManager: TextureManager) {
    /** Display lists, one per char (renders quad with char's uv coords). */
    private val charLists = GLAllocation.generateDisplayLists(256)
    RenderState.checkError("MonospaceFontRenderer.charLists")

    /** Buffer filled with char display lists to efficiently draw strings. */
    private val listBuffer = GLAllocation.createDirectIntBuffer(512)
    RenderState.checkError("MonospaceFontRenderer.listBuffer")

    private val (charWidth, charHeight) = (MonospaceFontRenderer.fontWidth * 2, MonospaceFontRenderer.fontHeight * 2)
    private val cols = 256 / charWidth
    private val uStep = charWidth / 256.0
    private val uSize = uStep
    private val vStep = (charHeight + 1) / 256.0
    private val vSize = charHeight / 256.0

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
        t.addVertexWithUV(0, charHeight, 0, u, v + vSize)
        t.addVertexWithUV(charWidth, charHeight, 0, u + uSize, v + vSize)
        t.addVertexWithUV(charWidth, 0, 0, u + uSize, v)
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

    def drawString(x: Int, y: Int, value: Array[Char], color: Array[Short], depth: PackedColor.Depth.Value) = {
      if (color.length != value.length) throw new IllegalArgumentException("Color count must match char count.")

      if (Settings.get.textAntiAlias)
        textureManager.bindTexture(MonospaceFontRenderer.font)
      else
        textureManager.bindTexture(MonospaceFontRenderer.fontAliased)
      GL11.glPushMatrix()
      GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT)
      GL11.glTranslatef(x, y, 0)
      GL11.glScalef(0.5f, 0.5f, 1)
      GL11.glDepthMask(false)

      // Background first. We try to merge adjacent backgrounds of the same
      // color to reduce the number of quads we have to draw.
      var cbg = 0x000000
      var offset = 0
      var width = 0
      for (col <- color.map(PackedColor.unpackBackground(_, depth))) {
        if (col != cbg) {
          draw(cbg, offset, width)
          cbg = col
          offset += width
          width = 0
        }
        width = width + 1
      }
      draw(cbg, offset, width)

      if (Settings.get.textLinearFiltering) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
      }

      // Foreground second. We only have to flush when the color changes, so
      // unless every char has a different color this should be quite efficient.
      var cfg = -1
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
            ((cfg & 0xFF0000) >> 16).toByte,
            ((cfg & 0x00FF00) >> 8).toByte,
            ((cfg & 0x0000FF) >> 0).toByte)
        }
        listBuffer.put(charLists + index)
        if (listBuffer.remaining == 0)
          flush()
      }
      flush()

      GL11.glPopAttrib()
      GL11.glPopMatrix()
    }

    private val bgu1 = 254.0 / 256.0
    private val bgu2 = 255.0 / 256.0
    private val bgv1 = 255.0 / 256.0
    private val bgv2 = 256.0 / 256.0

    private def draw(color: Int, offset: Int, width: Int) = if (color != 0 && width > 0) {
      val t = Tessellator.instance
      t.startDrawingQuads()
      t.setColorOpaque_I(color)
      t.addVertexWithUV(charWidth * offset, charHeight, 0, bgu1, bgv2)
      t.addVertexWithUV(charWidth * (offset + width), charHeight, 0, bgu2, bgv2)
      t.addVertexWithUV(charWidth * (offset + width), 0, 0, bgu2, bgv1)
      t.addVertexWithUV(charWidth * offset, 0, 0, bgu1, bgv1)
      t.draw()
    }

    private def flush() = if (listBuffer.position > 0) {
      listBuffer.flip()
      GL11.glCallLists(listBuffer)
      listBuffer.clear()
    }
  }

}