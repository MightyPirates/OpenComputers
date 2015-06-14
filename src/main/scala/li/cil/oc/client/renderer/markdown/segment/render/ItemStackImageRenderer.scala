package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12

private[markdown] class ItemStackImageRenderer(val stacks: Array[ItemStack]) extends ImageRenderer {
  // How long to show individual stacks, in milliseconds, before switching to the next.
  final val cycleSpeed = 1000

  override def getWidth = 32

  override def getHeight = 32

  override def render(mouseX: Int, mouseY: Int): Unit = {
    val mc = Minecraft.getMinecraft
    val index = (System.currentTimeMillis() % (cycleSpeed * stacks.length)).toInt / cycleSpeed
    val stack = stacks(index)

    GL11.glScalef(getWidth / 16, getHeight / 16, getWidth / 16)
    GL11.glEnable(GL12.GL_RESCALE_NORMAL)
    RenderHelper.enableGUIStandardItemLighting()
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240)
    RenderItem.getInstance.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager, stack, 0, 0)
    RenderHelper.disableStandardItemLighting()
  }
}
