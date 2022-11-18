package li.cil.oc.client.renderer.item

import com.google.common.collect.ImmutableList
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.integration.opencomputers.Item
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.item.ItemStack
import net.minecraft.util.math.vector.Vector3f
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

  def render(matrix: MatrixStack, buffer: IRenderTypeBuffer, stack: ItemStack, mountPoint: MountPoint): Unit = {
    val descriptor = api.Items.get(stack)

    if (descriptor == api.Items.get(Constants.ItemName.CraftingUpgrade)) {
      drawSimpleBlock(matrix, buffer.getBuffer(RenderTypes.UPGRADE_CRAFTING), mountPoint)

      RenderState.checkError(getClass.getName + ".renderItem: crafting upgrade")
    }

    else if (descriptor == api.Items.get(Constants.ItemName.GeneratorUpgrade)) {
      drawSimpleBlock(matrix, buffer.getBuffer(RenderTypes.UPGRADE_GENERATOR), mountPoint, if (Item.dataTag(stack).getInt("remainingTicks") > 0) 0.5f else 0)

      RenderState.checkError(getClass.getName + ".renderItem: generator upgrade")
    }

    else if (descriptor == api.Items.get(Constants.ItemName.InventoryUpgrade)) {
      drawSimpleBlock(matrix, buffer.getBuffer(RenderTypes.UPGRADE_INVENTORY), mountPoint)

      RenderState.checkError(getClass.getName + ".renderItem: inventory upgrade")
    }
  }

  private val (minX, minY, minZ) = (-0.1f, -0.1f, -0.1f)
  private val (maxX, maxY, maxZ) = (0.1f, 0.1f, 0.1f)

  private def drawSimpleBlock(stack: MatrixStack, r: IVertexBuilder, mountPoint: MountPoint, frontOffset: Float = 0) {
    stack.mulPose(new Vector3f(mountPoint.rotation.x, mountPoint.rotation.y, mountPoint.rotation.z).rotationDegrees(mountPoint.rotation.w))
    stack.translate(mountPoint.offset.x, mountPoint.offset.y, mountPoint.offset.z)

    // Front.
    r.vertex(stack.last.pose, minX, minY, maxZ).uv(frontOffset, 0.5f).normal(stack.last.normal, 0, 0, 1).endVertex()
    r.vertex(stack.last.pose, maxX, minY, maxZ).uv(frontOffset + 0.5f, 0.5f).normal(stack.last.normal, 0, 0, 1).endVertex()
    r.vertex(stack.last.pose, maxX, maxY, maxZ).uv(frontOffset + 0.5f, 0).normal(stack.last.normal, 0, 0, 1).endVertex()
    r.vertex(stack.last.pose, minX, maxY, maxZ).uv(frontOffset, 0).normal(stack.last.normal, 0, 0, 1).endVertex()

    // Top.
    r.vertex(stack.last.pose, maxX, maxY, maxZ).uv(1, 0.5f).normal(stack.last.normal, 0, 1, 0).endVertex()
    r.vertex(stack.last.pose, maxX, maxY, minZ).uv(1, 1).normal(stack.last.normal, 0, 1, 0).endVertex()
    r.vertex(stack.last.pose, minX, maxY, minZ).uv(0.5f, 1).normal(stack.last.normal, 0, 1, 0).endVertex()
    r.vertex(stack.last.pose, minX, maxY, maxZ).uv(0.5f, 0.5f).normal(stack.last.normal, 0, 1, 0).endVertex()

    // Bottom.
    r.vertex(stack.last.pose, minX, minY, maxZ).uv(0.5f, 0.5f).normal(stack.last.normal, 0, -1, 0).endVertex()
    r.vertex(stack.last.pose, minX, minY, minZ).uv(0.5f, 1).normal(stack.last.normal, 0, -1, 0).endVertex()
    r.vertex(stack.last.pose, maxX, minY, minZ).uv(1, 1).normal(stack.last.normal, 0, -1, 0).endVertex()
    r.vertex(stack.last.pose, maxX, minY, maxZ).uv(1, 0.5f).normal(stack.last.normal, 0, -1, 0).endVertex()

    // Left.
    r.vertex(stack.last.pose, maxX, maxY, maxZ).uv(0, 0.5f).normal(stack.last.normal, 1, 0, 0).endVertex()
    r.vertex(stack.last.pose, maxX, minY, maxZ).uv(0, 1).normal(stack.last.normal, 1, 0, 0).endVertex()
    r.vertex(stack.last.pose, maxX, minY, minZ).uv(0.5f, 1).normal(stack.last.normal, 1, 0, 0).endVertex()
    r.vertex(stack.last.pose, maxX, maxY, minZ).uv(0.5f, 0.5f).normal(stack.last.normal, 1, 0, 0).endVertex()

    // Right.
    r.vertex(stack.last.pose, minX, minY, maxZ).uv(0, 1).normal(stack.last.normal, -1, 0, 0).endVertex()
    r.vertex(stack.last.pose, minX, maxY, maxZ).uv(0, 0.5f).normal(stack.last.normal, -1, 0, 0).endVertex()
    r.vertex(stack.last.pose, minX, maxY, minZ).uv(0.5f, 0.5f).normal(stack.last.normal, -1, 0, 0).endVertex()
    r.vertex(stack.last.pose, minX, minY, minZ).uv(0.5f, 1).normal(stack.last.normal, -1, 0, 0).endVertex()
  }
}
