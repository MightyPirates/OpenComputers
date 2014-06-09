package li.cil.oc.client.renderer.item

import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.client.Textures
import li.cil.oc.server.driver.item.Item
import li.cil.oc.{Settings, api}
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.{RenderManager, RenderItem}
import net.minecraft.client.renderer.Tessellator
import net.minecraft.item.ItemStack
import net.minecraft.util.{EnumChatFormatting, AxisAlignedBB}
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRendererHelper, ItemRenderType}
import org.lwjgl.opengl.GL11
import scala.collection.convert.WrapAsScala._

object ItemRenderer extends IItemRenderer {
  val renderItem = new RenderItem()
  renderItem.setRenderManager(RenderManager.instance)

  val bounds = AxisAlignedBB.getBoundingBox(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)

  def isUpgrade(descriptor: ItemInfo) =
    descriptor == api.Items.get("craftingUpgrade") ||
      descriptor == api.Items.get("generatorUpgrade") ||
      descriptor == api.Items.get("inventoryUpgrade")

  def isFloppy(descriptor: ItemInfo) =
    descriptor == api.Items.get("floppy") ||
      descriptor == api.Items.get("lootDisk") ||
      descriptor == api.Items.get("openOS")

  override def handleRenderType(stack: ItemStack, renderType: ItemRenderType) =
    (renderType == ItemRenderType.EQUIPPED && isUpgrade(api.Items.get(stack))) ||
      (renderType == ItemRenderType.INVENTORY && isFloppy(api.Items.get(stack)))

  override def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) =
  // Note: it's easier to revert changes introduced by this "helper" than by
  // the code that applies if no helper is used...
    helper == ItemRendererHelper.EQUIPPED_BLOCK

  override def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*) {
    val mc = Minecraft.getMinecraft
    val tm = mc.getTextureManager
    val descriptor = api.Items.get(stack)
    if (isUpgrade(descriptor)) {

      // Revert offset introduced by the render "helper".
      GL11.glTranslatef(0.5f, 0.5f, 0.5f)

      if (descriptor == api.Items.get("craftingUpgrade")) {
        tm.bindTexture(Textures.upgradeCrafting)
        drawSimpleBlock()
      }

      else if (descriptor == api.Items.get("generatorUpgrade")) {
        tm.bindTexture(Textures.upgradeGenerator)
        drawSimpleBlock(if (Item.dataTag(stack).getInteger("remainingTicks") > 0) 0.5 else 0)
      }

      else if (descriptor == api.Items.get("inventoryUpgrade")) {
        tm.bindTexture(Textures.upgradeInventory)
        drawSimpleBlock()
      }
    }

    else if (isFloppy(descriptor)) {
      renderItem.renderItemIntoGUI(null, tm, stack, 0, 0)
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

  private def drawSimpleBlock(frontOffset: Double = 0) {
    val t = Tessellator.instance
    t.startDrawingQuads()

    // Front.
    t.setNormal(0, 0, 1)
    t.addVertexWithUV(bounds.minX, bounds.minY, bounds.maxZ, frontOffset, 0.5)
    t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.maxZ, frontOffset + 0.5, 0.5)
    t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.maxZ, frontOffset + 0.5, 0)
    t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.maxZ, frontOffset, 0)

    // Top.
    t.setNormal(0, 1, 0)
    t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.maxZ, 1, 0.5)
    t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.minZ, 1, 1)
    t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.minZ, 0.5, 1)
    t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.maxZ, 0.5, 0.5)

    // Bottom.
    t.setNormal(0, -1, 0)
    t.addVertexWithUV(bounds.minX, bounds.minY, bounds.maxZ, 0.5, 0.5)
    t.addVertexWithUV(bounds.minX, bounds.minY, bounds.minZ, 0.5, 1)
    t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.minZ, 1, 1)
    t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.maxZ, 1, 0.5)

    // Left.
    t.setNormal(1, 0, 0)
    t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.maxZ, 0, 0.5)
    t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.maxZ, 0, 1)
    t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.minZ, 0.5, 1)
    t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.minZ, 0.5, 0.5)

    // Right.
    t.setNormal(-1, 0, 0)
    t.addVertexWithUV(bounds.minX, bounds.minY, bounds.maxZ, 0, 1)
    t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.maxZ, 0, 0.5)
    t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.minZ, 0.5, 0.5)
    t.addVertexWithUV(bounds.minX, bounds.minY, bounds.minZ, 0.5, 1)

    t.draw()
  }
}
