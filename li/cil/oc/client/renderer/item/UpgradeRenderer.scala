package li.cil.oc.client.renderer.item

import li.cil.oc.server.driver.item.Item
import li.cil.oc.{Settings, Items}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.item.ItemStack
import net.minecraft.util.{ResourceLocation, AxisAlignedBB}
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRendererHelper, ItemRenderType}
import org.lwjgl.opengl.GL11

object UpgradeRenderer extends IItemRenderer {
  def handleRenderType(item: ItemStack, renderType: ItemRenderType) = {
    Items.multi.subItem(item) match {
      case Some(subItem) if subItem == Items.generator || subItem == Items.crafting => renderType == ItemRenderType.EQUIPPED
      case _ => false
    }
  }

  def shouldUseRenderHelper(renderType: ItemRenderType, stack: ItemStack, helper: ItemRendererHelper) =
  // Note: it's easier to revert changes introduced by this "helper" than by
  // the code that applies if no helper is used...
    helper == ItemRendererHelper.EQUIPPED_BLOCK

  def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*) {
    val tm = Minecraft.getMinecraft.getTextureManager
    val t = Tessellator.instance

    // Revert offset introduced by the render "helper".
    GL11.glTranslatef(0.5f, 0.5f, 0.5f)

    Items.multi.subItem(stack) match {
      case Some(subItem) if subItem == Items.crafting =>
        // TODO display list?
        val b = AxisAlignedBB.getAABBPool.getAABB(0.4, 0.2, 1-0.36, 0.6, 0.4, 1-0.16)
        tm.bindTexture(new ResourceLocation(Settings.resourceDomain, "textures/items/upgrade_crafting_equipped.png"))

        // Front.
        t.startDrawingQuads()
        t.addVertexWithUV(b.minX, b.minY, b.maxZ, 0, 0.5)
        t.addVertexWithUV(b.maxX, b.minY, b.maxZ, 0.5, 0.5)
        t.addVertexWithUV(b.maxX, b.maxY, b.maxZ, 0.5, 0)
        t.addVertexWithUV(b.minX, b.maxY, b.maxZ, 0, 0)
        t.setNormal(0, 0, 1)
        t.draw()

        // Bottom.
        t.startDrawingQuads()
        t.addVertexWithUV(b.minX, b.minY, b.maxZ, 0.5, 0.5)
        t.addVertexWithUV(b.minX, b.minY, b.minZ, 0.5, 1)
        t.addVertexWithUV(b.maxX, b.minY, b.minZ, 1, 1)
        t.addVertexWithUV(b.maxX, b.minY, b.maxZ, 1, 0.5)
        t.setNormal(0, -1, 0)
        t.draw()

        // Left.
        t.startDrawingQuads()
        t.addVertexWithUV(b.maxX, b.maxY, b.maxZ, 0, 0.5)
        t.addVertexWithUV(b.maxX, b.minY, b.maxZ, 0, 1)
        t.addVertexWithUV(b.maxX, b.minY, b.minZ, 0.5, 1)
        t.addVertexWithUV(b.maxX, b.maxY, b.minZ, 0.5, 0.5)
        t.setNormal(1, 0, 0)
        t.draw()

        // Right.
        t.startDrawingQuads()
        t.addVertexWithUV(b.minX, b.minY, b.maxZ, 0, 1)
        t.addVertexWithUV(b.minX, b.maxY, b.maxZ, 0, 0.5)
        t.addVertexWithUV(b.minX, b.maxY, b.minZ, 0.5, 0.5)
        t.addVertexWithUV(b.minX, b.minY, b.minZ, 0.5, 1)
        t.setNormal(-1, 0, 0)
        t.draw()
      case Some(subItem) if subItem == Items.generator =>
        // TODO display lists?
        val onOffset = if (Item.dataTag(stack).getInteger("remainingTicks") > 0) 0.5 else 0
        val b = AxisAlignedBB.getAABBPool.getAABB(0.4, 0.2, 0.16, 0.6, 0.4, 0.36)
        tm.bindTexture(new ResourceLocation(Settings.resourceDomain, "textures/items/upgrade_generator_equipped.png"))

        // Back.
        t.startDrawingQuads()
        t.addVertexWithUV(b.minX, b.minY, b.minZ, onOffset, 0.5)
        t.addVertexWithUV(b.minX, b.maxY, b.minZ, onOffset, 0)
        t.addVertexWithUV(b.maxX, b.maxY, b.minZ, onOffset + 0.5, 0)
        t.addVertexWithUV(b.maxX, b.minY, b.minZ, onOffset + 0.5, 0.5)
        t.setNormal(0, 0, -1)
        t.draw()

        // Bottom.
        t.startDrawingQuads()
        t.addVertexWithUV(b.minX, b.minY, b.minZ, 0.5, 0.5)
        t.addVertexWithUV(b.maxX, b.minY, b.minZ, 1, 0.5)
        t.addVertexWithUV(b.maxX, b.minY, b.maxZ, 1, 1)
        t.addVertexWithUV(b.minX, b.minY, b.maxZ, 0.5, 1)
        t.setNormal(0, -1, 0)
        t.draw()

        // Left.
        t.startDrawingQuads()
        t.addVertexWithUV(b.maxX, b.minY, b.minZ, 0, 1)
        t.addVertexWithUV(b.maxX, b.maxY, b.minZ, 0, 0.5)
        t.addVertexWithUV(b.maxX, b.maxY, b.maxZ, 0.5, 0.5)
        t.addVertexWithUV(b.maxX, b.minY, b.maxZ, 0.5, 1)
        t.setNormal(1, 0, 0)
        t.draw()

        // Right.
        t.startDrawingQuads()
        t.addVertexWithUV(b.minX, b.minY, b.minZ, 0, 1)
        t.addVertexWithUV(b.minX, b.minY, b.maxZ, 0.5, 1)
        t.addVertexWithUV(b.minX, b.maxY, b.maxZ, 0.5, 0.5)
        t.addVertexWithUV(b.minX, b.maxY, b.minZ, 0, 0.5)
        t.setNormal(-1, 0, 0)
        t.draw()
      case _ =>
    }
  }
}
