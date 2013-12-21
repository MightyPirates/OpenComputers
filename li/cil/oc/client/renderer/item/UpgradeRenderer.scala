package li.cil.oc.client.renderer.item

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
      case Some(subItem) if subItem == Items.generator => renderType == ItemRenderType.EQUIPPED
      case _ => false
    }
  }

  def shouldUseRenderHelper(renderType: ItemRenderType, item: ItemStack, helper: ItemRendererHelper) = helper == ItemRendererHelper.EQUIPPED_BLOCK

  def renderItem(renderType: ItemRenderType, item: ItemStack, data: AnyRef*) {
    val tm = Minecraft.getMinecraft.getTextureManager
    GL11.glTranslatef(0.5f, 0.5f, 0.5f)
    val t = Tessellator.instance

    Items.multi.subItem(item) match {
      case Some(subItem) if subItem == Items.generator =>
        // TODO display lists?
        val onOffset = if (true) 0.5 else 0
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
