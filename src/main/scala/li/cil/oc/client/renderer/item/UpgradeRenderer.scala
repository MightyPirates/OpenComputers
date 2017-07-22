package li.cil.oc.client.renderer.item

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.client.Textures
import li.cil.oc.integration.opencomputers.Item
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
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
      Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.upgradeCrafting)
      drawSimpleBlock(mountPoint)

      RenderState.checkError(getClass.getName + ".renderItem: crafting upgrade")
    }

    else if (descriptor == api.Items.get(Constants.ItemName.GeneratorUpgrade)) {
      Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.upgradeGenerator)
      drawSimpleBlock(mountPoint, if (Item.dataTag(stack).getInteger("remainingTicks") > 0) 0.5f else 0)

      RenderState.checkError(getClass.getName + ".renderItem: generator upgrade")
    }

    else if (descriptor == api.Items.get(Constants.ItemName.InventoryUpgrade)) {
      Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.upgradeInventory)
      drawSimpleBlock(mountPoint)

      RenderState.checkError(getClass.getName + ".renderItem: inventory upgrade")
    }
  }

  private val bounds = AxisAlignedBB.getBoundingBox(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1)

  private def drawSimpleBlock(mountPoint: MountPoint, frontOffset: Float = 0) {
    GL11.glRotatef(mountPoint.rotation.getW, mountPoint.rotation.getX, mountPoint.rotation.getY, mountPoint.rotation.getZ)
    GL11.glTranslatef(mountPoint.offset.getX, mountPoint.offset.getY, mountPoint.offset.getZ)

    GL11.glBegin(GL11.GL_QUADS)

    // Front.
    GL11.glNormal3f(0, 0, 1)
    GL11.glTexCoord2f(frontOffset, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(frontOffset + 0.5f, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(frontOffset + 0.5f, 0)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(frontOffset, 0)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)

    // Top.
    GL11.glNormal3f(0, 1, 0)
    GL11.glTexCoord2f(1, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)

    // Bottom.
    GL11.glNormal3f(0, -1, 0)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(1, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)

    // Left.
    GL11.glNormal3f(1, 0, 0)
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.maxX, bounds.minY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.maxX, bounds.maxY, bounds.minZ)

    // Right.
    GL11.glNormal3f(-1, 0, 0)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.maxZ)
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.maxZ)
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3d(bounds.minX, bounds.maxY, bounds.minZ)
    GL11.glTexCoord2f(0.5f, 1)
    GL11.glVertex3d(bounds.minX, bounds.minY, bounds.minZ)

    GL11.glEnd()
  }
}
