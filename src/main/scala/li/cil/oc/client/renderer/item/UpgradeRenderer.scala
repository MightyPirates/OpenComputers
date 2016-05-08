package li.cil.oc.client.renderer.item

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.client.Textures
import li.cil.oc.integration.opencomputers.Item
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemStack
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11

object UpgradeRenderer {
  lazy val craftingUpgrade = api.Items.get(Constants.ItemName.CraftingUpgrade)
  lazy val generatorUpgrade = api.Items.get(Constants.ItemName.GeneratorUpgrade)
  lazy val inventoryUpgrade = api.Items.get(Constants.ItemName.InventoryUpgrade)

  def preferredMountPoint(stack: ItemStack, availableMountPoints: java.util.Set[String]): String = {
    val descriptor = api.Items.get(stack)

    if (descriptor == craftingUpgrade || descriptor == generatorUpgrade || descriptor == inventoryUpgrade) {
      if (descriptor == generatorUpgrade && availableMountPoints.contains(MountPointName.BottomBack)) MountPointName.BottomBack
      else if (descriptor == inventoryUpgrade && availableMountPoints.contains(MountPointName.TopBack)) MountPointName.TopBack
      else MountPointName.Any
    }
    else MountPointName.None
  }

  def canRender(stack: ItemStack): Boolean = {
    val descriptor = api.Items.get(stack)

    descriptor == craftingUpgrade || descriptor == generatorUpgrade || descriptor == inventoryUpgrade
  }

  def render(stack: ItemStack, mountPoint: MountPoint): Unit = {
    val descriptor = api.Items.get(stack)

    if (descriptor == api.Items.get(Constants.ItemName.CraftingUpgrade)) {
      Textures.bind(Textures.Model.UpgradeCrafting)
      drawSimpleBlock(mountPoint)

      RenderState.checkError(getClass.getName + ".renderItem: crafting upgrade")
    }

    else if (descriptor == api.Items.get(Constants.ItemName.GeneratorUpgrade)) {
      Textures.bind(Textures.Model.UpgradeGenerator)
      drawSimpleBlock(mountPoint, if (Item.dataTag(stack).getInteger("remainingTicks") > 0) 0.5f else 0)

      RenderState.checkError(getClass.getName + ".renderItem: generator upgrade")
    }

    else if (descriptor == api.Items.get(Constants.ItemName.InventoryUpgrade)) {
      Textures.bind(Textures.Model.UpgradeInventory)
      drawSimpleBlock(mountPoint)

      RenderState.checkError(getClass.getName + ".renderItem: inventory upgrade")
    }
  }

  private val bounds = new AxisAlignedBB(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)

  private def drawSimpleBlock(mountPoint: MountPoint, frontOffset: Float = 0) {
    GlStateManager.rotate(mountPoint.rotation.getW, mountPoint.rotation.getX, mountPoint.rotation.getY, mountPoint.rotation.getZ)
    GlStateManager.translate(mountPoint.offset.getX, mountPoint.offset.getY, mountPoint.offset.getZ)

    val t = Tessellator.getInstance()
    val r = t.getBuffer
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL)

    // Front.
    r.pos(bounds.minX, bounds.minY, bounds.maxZ).tex(frontOffset, 0.5f).normal(0, 0, 1).endVertex()
    r.pos(bounds.maxX, bounds.minY, bounds.maxZ).tex(frontOffset + 0.5f, 0.5f).normal(0, 0, 1).endVertex()
    r.pos(bounds.maxX, bounds.maxY, bounds.maxZ).tex(frontOffset + 0.5f, 0).normal(0, 0, 1).endVertex()
    r.pos(bounds.minX, bounds.maxY, bounds.maxZ).tex(frontOffset, 0).normal(0, 0, 1).endVertex()

    // Top.
    r.pos(bounds.maxX, bounds.maxY, bounds.maxZ).tex(1, 0.5f).normal(0, 1, 0).endVertex()
    r.pos(bounds.maxX, bounds.maxY, bounds.minZ).tex(1, 1).normal(0, 1, 0).endVertex()
    r.pos(bounds.minX, bounds.maxY, bounds.minZ).tex(0.5f, 1).normal(0, 1, 0).endVertex()
    r.pos(bounds.minX, bounds.maxY, bounds.maxZ).tex(0.5f, 0.5f).normal(0, 1, 0).endVertex()

    // Bottom.
    r.pos(bounds.minX, bounds.minY, bounds.maxZ).tex(0.5f, 0.5f).normal(0, -1, 0).endVertex()
    r.pos(bounds.minX, bounds.minY, bounds.minZ).tex(0.5f, 1).normal(0, -1, 0).endVertex()
    r.pos(bounds.maxX, bounds.minY, bounds.minZ).tex(1, 1).normal(0, -1, 0).endVertex()
    r.pos(bounds.maxX, bounds.minY, bounds.maxZ).tex(1, 0.5f).normal(0, -1, 0).endVertex()

    // Left.
    r.pos(bounds.maxX, bounds.maxY, bounds.maxZ).tex(0, 0.5f).normal(1, 0, 0).endVertex()
    r.pos(bounds.maxX, bounds.minY, bounds.maxZ).tex(0, 1).normal(1, 0, 0).endVertex()
    r.pos(bounds.maxX, bounds.minY, bounds.minZ).tex(0.5f, 1).normal(1, 0, 0).endVertex()
    r.pos(bounds.maxX, bounds.maxY, bounds.minZ).tex(0.5f, 0.5f).normal(1, 0, 0).endVertex()

    // Right.
    r.pos(bounds.minX, bounds.minY, bounds.maxZ).tex(0, 1).normal(-1, 0, 0).endVertex()
    r.pos(bounds.minX, bounds.maxY, bounds.maxZ).tex(0, 0.5f).normal(-1, 0, 0).endVertex()
    r.pos(bounds.minX, bounds.maxY, bounds.minZ).tex(0.5f, 0.5f).normal(-1, 0, 0).endVertex()
    r.pos(bounds.minX, bounds.minY, bounds.minZ).tex(0.5f, 1).normal(-1, 0, 0).endVertex()

    t.draw()
  }
}
