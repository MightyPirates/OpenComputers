package li.cil.oc.client.gui

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

@SideOnly(Side.CLIENT)
class ImageButton(id: Int, x: Int, y: Int, w: Int, h: Int,
                  val image: ResourceLocation, text: String = null,
                  val canToggle: Boolean = false,
                  val textColor: Int = 0xE0E0E0,
                  val textDisabledColor: Int = 0xA0A0A0,
                  val textHoverColor: Int = 0xFFFFA0,
                  val textIndent: Int = -1) extends GuiButton(id, x, y, w, h, text) {

  var toggled = false

  var hoverOverride = false

  override def drawButton(mc: Minecraft, mouseX: Int, mouseY: Int) {
    if (drawButton) {
      mc.renderEngine.bindTexture(image)
      GL11.glColor4f(1, 1, 1, 1)
      field_82253_i = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height

      val x0 = xPosition
      val x1 = xPosition + width
      val y0 = yPosition
      val y1 = yPosition + height

      val u0 = if (toggled) 0.5 else 0
      val u1 = u0 + (if (canToggle) 0.5 else 1)
      val v0 = if (hoverOverride || getHoverState(field_82253_i) == 2) 0.5 else 0
      val v1 = v0 + 0.5

      val t = Tessellator.instance
      t.startDrawingQuads()
      t.addVertexWithUV(x0, y1, zLevel, u0, v1)
      t.addVertexWithUV(x1, y1, zLevel, u1, v1)
      t.addVertexWithUV(x1, y0, zLevel, u1, v0)
      t.addVertexWithUV(x0, y0, zLevel, u0, v0)
      t.draw()

      if (displayString != null) {
        val color =
          if (!enabled) textDisabledColor
          else if (hoverOverride || field_82253_i) textHoverColor
          else textColor
        if (textIndent >= 0) drawString(mc.fontRenderer, displayString, textIndent + xPosition, yPosition + (height - 8) / 2, color)
        else drawCenteredString(mc.fontRenderer, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, color)
      }
    }
  }
}
