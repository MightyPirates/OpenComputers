package li.cil.oc.client.renderer.item

import li.cil.oc.{Settings, api}
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRendererHelper, ItemRenderType}
import net.minecraft.item.ItemStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.{RenderItem, RenderManager}
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import scala.collection.convert.WrapAsScala._
import net.minecraft.util.EnumChatFormatting

object FloppyRenderer extends IItemRenderer {
  val renderItem = new RenderItem()
  renderItem.setRenderManager(RenderManager.instance)

  override def handleRenderType(stack: ItemStack, renderType: ItemRenderType) = renderType == ItemRenderType.INVENTORY && {
    val descriptor = api.Items.get(stack)
    descriptor == api.Items.get("floppy") ||
      descriptor == api.Items.get("lootDisk") ||
      descriptor == api.Items.get("openOS")
  }

  override def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) = false

  override def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*) {
    val mc = Minecraft.getMinecraft
    renderItem.renderItemIntoGUI(null, mc.getTextureManager, stack, 0, 0)
    val res = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight)
    val fontRenderer = renderItem.getFontRendererFromRenderManager
    if (fontRenderer != null && res.getScaleFactor > 1) {
      GL11.glPushMatrix()
      GL11.glTranslatef(4f + 2f / res.getScaleFactor, 9f + 2f / res.getScaleFactor, 0)
      GL11.glScalef(1f / res.getScaleFactor, 1f / res.getScaleFactor, 1f)
      val maxLength = (res.getScaleFactor * 7.5).toInt
      val label =
        if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data") && stack.getTagCompound.getCompoundTag(Settings.namespace + "data").hasKey(Settings.namespace + "fs.label")) {
          stack.getTagCompound.getCompoundTag(Settings.namespace + "data").getString(Settings.namespace + "fs.label")
        }
        else "disk"
      val lines = fontRenderer.listFormattedStringToWidth(EnumChatFormatting.getTextWithoutFormattingCodes(label), maxLength).take(math.max(1, res.getScaleFactor / 2))
      for (line <- lines) {
        fontRenderer.drawString(line.asInstanceOf[String], 0, 0, 0)
        GL11.glTranslatef(0, fontRenderer.FONT_HEIGHT, 0)
      }
      GL11.glPopMatrix()
    }
  }
}
