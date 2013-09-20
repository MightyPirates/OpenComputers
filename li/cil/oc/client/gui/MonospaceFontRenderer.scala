package li.cil.oc.client.gui

import net.minecraft.util.ResourceLocation
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.client.renderer.Tessellator
import org.lwjgl.opengl.GL11
import net.minecraft.client.renderer.GLAllocation
import java.nio.charset.Charset

object MonospaceFontRenderer {
  private val font = new ResourceLocation("opencomputers", "textures/font/ascii.png")

  private var instance: Option[Renderer] = None

  def init(textureManager: TextureManager) =
    instance = instance.orElse(Some(new Renderer(textureManager)))

  val (fontWidth, fontHeight) = (5, 9)

  def drawString(value: Array[Char], x: Int, y: Int) = instance match {
    case None => // Do nothing, not initialized.
    case Some(renderer) => renderer.drawString(value, x, y)
  }

  private class Renderer(private val textureManager: TextureManager) {
    /** Display lists, one per char (renders quad with char's uv coords). */
    private val charLists = GLAllocation.generateDisplayLists(256)

    /** Buffer filled with char display lists to efficiently draw strings. */
    private val listBuffer = GLAllocation.createDirectIntBuffer(512)

    // Set up the display lists.
    {
      // The font texture is 16x12, but chars are really only 10x12.
      val charsPerRow = 16
      val (charWidth, charHeight) = (MonospaceFontRenderer.fontWidth * 2, MonospaceFontRenderer.fontHeight * 2)
      val uStep = 1.0 / charsPerRow
      val vStep = 1.0 / 12 * 240 / 256 // Correct for padding at bottom.
      val uOffset = uStep * 3 / 16
      val uSize = uStep * 10 / 16
      val vOffset = vStep * 1 / 20
      val vSize = vStep * 18 / 20
      val t = Tessellator.instance
      // Special case for whitespace: just translate, don't render.
      GL11.glNewList(charLists + 32, GL11.GL_COMPILE)
      GL11.glTranslatef(charWidth, 0, 0)
      GL11.glEndList()
      // Now create lists for all printable chars.
      for (index <- (33 until 0x7F).union(0x9F until 0xFF)) {
        // The font texture does not contain the 0-1F range, nor the 0x7F to
        // 0x9F range (control chars as per Character.isISOControl).
        val textureIndex =
          (if (index - 32 >= 0x7F) index - (0x9F - 0x7F) else index) - 32
        val x = textureIndex % charsPerRow
        val y = textureIndex / charsPerRow
        val u = x * uStep + uOffset
        val v = y * vStep + vOffset
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
    }

    def drawString(value: Array[Char], x: Int, y: Int) = {
      setTexture(textureManager, MonospaceFontRenderer.font)
      GL11.glPushMatrix()
      GL11.glTranslatef(x, y, 0)
      GL11.glScalef(0.5f, 0.5f, 1)
      GL11.glColor4f(1, 1, 1, 1)
      for (c <- value) {
        listBuffer.put(charLists + c)
        if (listBuffer.remaining == 0)
          flush()
      }
      flush()
      GL11.glPopMatrix()
    }

    private def flush() = {
      listBuffer.flip()
      GL11.glCallLists(listBuffer)
      listBuffer.clear()
    }
  }

  private def setTexture(tm: TextureManager, resource: ResourceLocation) = tm.func_110577_a(resource)
}