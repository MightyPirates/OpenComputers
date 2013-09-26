package li.cil.oc.client.gui

import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import scala.io.Source

object MonospaceFontRenderer {
  private val font = new ResourceLocation("opencomputers", "textures/font/chars.png")

  private val chars = Source.fromInputStream(MonospaceFontRenderer.getClass.getResourceAsStream("/assets/opencomputers/textures/font/chars.txt")).mkString

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
      val (charWidth, charHeight) = (MonospaceFontRenderer.fontWidth * 2, MonospaceFontRenderer.fontHeight * 2)
      val cols = 256 / charWidth
      val uStep = charWidth / 256.0
      val vStep = charHeight / 256.0
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

    def drawString(value: Array[Char], x: Int, y: Int) = {
      setTexture(textureManager, MonospaceFontRenderer.font)
      GL11.glPushMatrix()
      GL11.glTranslatef(x, y, 0)
      GL11.glScalef(0.5f, 0.5f, 1)
      GL11.glColor4f(1, 1, 1, 1)
      for (c <- value) {
        val index = 1 + chars.indexOf(c) match {
          case -1 => chars.indexOf('?')
          case i => i
        }
        listBuffer.put(charLists + index)
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