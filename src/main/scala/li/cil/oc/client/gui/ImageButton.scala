package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.AbstractGui
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.client.gui.widget.button.Button.IPressable
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11

@OnlyIn(Dist.CLIENT)
class ImageButton(xPos: Int, yPos: Int, w: Int, h: Int,
                  handler: IPressable,
                  val image: ResourceLocation = null,
                  text: ITextComponent = StringTextComponent.EMPTY,
                  val canToggle: Boolean = false,
                  val textColor: Int = 0xE0E0E0,
                  val textDisabledColor: Int = 0xA0A0A0,
                  val textHoverColor: Int = 0xFFFFA0,
                  val textIndent: Int = -1) extends Button(xPos, yPos, w, h, text, handler) {

  var toggled = false

  var hoverOverride = false

  override def renderButton(stack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
    if (visible) {
      Textures.bind(image)
      RenderSystem.color4f(1, 1, 1, 1)
      isHovered = isMouseOver(mouseX, mouseY)

      val x0 = x
      val x1 = x + width
      val y0 = y
      val y1 = y + height

      val drawHover = hoverOverride || getYImage(isHovered) == 2

      val t = Tessellator.getInstance
      val r = t.getBuilder
      if (image != null) {
        val u0 = if (toggled) 0.5f else 0
        val u1 = u0 + (if (canToggle) 0.5f else 1)
        val v0 = if (drawHover) 0.5f else 0
        val v1 = v0 + 0.5f

        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        r.vertex(stack.last.pose, x0, y1, getBlitOffset).uv(u0, v1).endVertex()
        r.vertex(stack.last.pose, x1, y1, getBlitOffset).uv(u1, v1).endVertex()
        r.vertex(stack.last.pose, x1, y0, getBlitOffset).uv(u1, v0).endVertex()
        r.vertex(stack.last.pose, x0, y0, getBlitOffset).uv(u0, v0).endVertex()
      }
      else {
        if (drawHover) {
          RenderSystem.color4f(1, 1, 1, 0.8f)
        }
        else {
          RenderSystem.color4f(1, 1, 1, 0.4f)
        }

        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        r.vertex(stack.last.pose, x0, y1, getBlitOffset).endVertex()
        r.vertex(stack.last.pose, x1, y1, getBlitOffset).endVertex()
        r.vertex(stack.last.pose, x1, y0, getBlitOffset).endVertex()
        r.vertex(stack.last.pose, x0, y0, getBlitOffset).endVertex()
      }
      t.end()

      if (getMessage != StringTextComponent.EMPTY) {
        val color =
          if (!active) textDisabledColor
          else if (hoverOverride || isHovered) textHoverColor
          else textColor
        val mc = Minecraft.getInstance
        if (textIndent >= 0) AbstractGui.drawString(stack, mc.font, getMessage, textIndent + x, y + (height - 8) / 2, color)
        else AbstractGui.drawCenteredString(stack, mc.font, getMessage, x + width / 2, y + (height - 8) / 2, color)
      }
    }
  }
}
