package li.cil.oc.client.renderer.markdown.segment.render

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13

private[markdown] class ItemStackImageRenderer(val stacks: Array[ItemStack]) extends ImageRenderer {
  // How long to show individual stacks, in milliseconds, before switching to the next.
  final val cycleSpeed = 1000

  override def getWidth = 32

  override def getHeight = 32

  override def render(matrix: MatrixStack, mouseX: Int, mouseY: Int): Unit = {
    val mc = Minecraft.getInstance
    val index = (System.currentTimeMillis() % (cycleSpeed * stacks.length)).toInt / cycleSpeed
    val stack = stacks(index)

    matrix.scale(getWidth / 16, getHeight / 16, getWidth / 16)
    // Translate manually because ItemRenderer generally can't take a MatrixStack.
    RenderSystem.pushMatrix()
    RenderSystem.multMatrix(matrix.last().pose())
    RenderSystem.enableRescaleNormal()
    RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240, 240)
    mc.getItemRenderer.renderAndDecorateItem(stack, 0, 0)
    RenderSystem.popMatrix()
  }
}
