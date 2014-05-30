package li.cil.oc.client.renderer.item

import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.server.driver.item.Item
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRendererHelper, ItemRenderType}
import org.lwjgl.opengl.GL11

object UpgradeRenderer extends IItemRenderer {
  val bounds = AxisAlignedBB.getBoundingBox(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)
  
  override def handleRenderType(stack: ItemStack, renderType: ItemRenderType) = renderType == ItemRenderType.EQUIPPED && {
    val descriptor = api.Items.get(stack)
    descriptor == api.Items.get("craftingUpgrade") ||
      descriptor == api.Items.get("generatorUpgrade") ||
      descriptor == api.Items.get("inventoryUpgrade")
  }

  override def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) =
  // Note: it's easier to revert changes introduced by this "helper" than by
  // the code that applies if no helper is used...
    helper == ItemRendererHelper.EQUIPPED_BLOCK

  override def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*) {
    val descriptor = api.Items.get(stack)
    val tm = Minecraft.getMinecraft.getTextureManager

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
