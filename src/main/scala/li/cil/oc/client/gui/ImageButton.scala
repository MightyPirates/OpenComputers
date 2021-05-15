package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11

@SideOnly(Side.CLIENT)
class ImageButton(id: Int, xPos: Int, yPos: Int, w: Int, h: Int,
                  val image: ResourceLocation = null,
                  text: String = null,
                  val canToggle: Boolean = false,
                  val textColor: Int = 0xE0E0E0,
                  val textDisabledColor: Int = 0xA0A0A0,
                  val textHoverColor: Int = 0xFFFFA0,
                  val textIndent: Int = -1) extends GuiButton(id, xPos, yPos, w, h, text) {

  var toggled = false

  var hoverOverride = false

  override def drawButton(mc: Minecraft, mouseX: Int, mouseY: Int, partialTicks: Float) {
    if (visible) {
      Textures.bind(image)
      GlStateManager.color(1, 1, 1, 1)
      hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height

      val x0 = x
      val x1 = x + width
      val y0 = y
      val y1 = y + height

      val isHovered = hoverOverride || getHoverState(isMouseOver) == 2

      val t = Tessellator.getInstance
      val r = t.getBuffer
      if (image != null) {
        val u0 = if (toggled) 0.5 else 0
        val u1 = u0 + (if (canToggle) 0.5 else 1)
        val v0 = if (hoverOverride || getHoverState(hovered) == 2) 0.5 else 0
        val v1 = v0 + 0.5

        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        r.pos(x0, y1, zLevel).tex(u0, v1).endVertex()
        r.pos(x1, y1, zLevel).tex(u1, v1).endVertex()
        r.pos(x1, y0, zLevel).tex(u1, v0).endVertex()
        r.pos(x0, y0, zLevel).tex(u0, v0).endVertex()
      }
      else if (isHovered) {
        GlStateManager.color(1, 1, 1, 0.8f)

        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        r.pos(x0, y1, zLevel).endVertex()
        r.pos(x1, y1, zLevel).endVertex()
        r.pos(x1, y0, zLevel).endVertex()
        r.pos(x0, y0, zLevel).endVertex()
      }
      else {
        GlStateManager.color(1, 1, 1, 0.4f)

        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        r.pos(x0, y1, zLevel).endVertex()
        r.pos(x1, y1, zLevel).endVertex()
        r.pos(x1, y0, zLevel).endVertex()
        r.pos(x0, y0, zLevel).endVertex()
      }
      t.draw()

      if (displayString != null) {
        val color =
          if (!enabled) textDisabledColor
          else if (hoverOverride || hovered) textHoverColor
          else textColor
        if (textIndent >= 0) drawString(mc.fontRenderer, displayString, textIndent + x, y + (height - 8) / 2, color)
        else drawCenteredString(mc.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, color)
      }
    }
  }
}
