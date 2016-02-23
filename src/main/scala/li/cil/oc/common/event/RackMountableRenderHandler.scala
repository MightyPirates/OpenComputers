package li.cil.oc.common.event

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.tileentity.RenderUtil
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object RackMountableRenderHandler {
  lazy val DiskDriveMountable = api.Items.get(Constants.ItemName.DiskDriveMountable)

  lazy val Servers = Array(
    api.Items.get(Constants.ItemName.ServerTier1),
    api.Items.get(Constants.ItemName.ServerTier2),
    api.Items.get(Constants.ItemName.ServerTier3),
    api.Items.get(Constants.ItemName.ServerCreative)
  )

  lazy val TerminalServer = api.Items.get(Constants.ItemName.TerminalServer)

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.TileEntity): Unit = {
    if (e.data != null && DiskDriveMountable == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Disk drive.

      if (e.data.hasKey("disk")) {
        val stack = ItemStack.loadItemStackFromNBT(e.data.getCompoundTag("disk"))
        if (stack != null) {
          GlStateManager.pushMatrix()
          GlStateManager.scale(1, -1, 1)
          GlStateManager.translate(10 / 16f, -(3.5f + e.mountable * 3f) / 16f, -2 / 16f)
          GlStateManager.rotate(90, -1, 0, 0)
          GlStateManager.scale(0.5f, 0.5f, 0.5f)

          val brightness = e.rack.world.getLightBrightnessForSkyBlocks(BlockPosition(e.rack).offset(e.rack.facing), 0)
          OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

          // This is very 'meh', but item frames do it like this, too!
          val entity = new EntityItem(e.rack.world, 0, 0, 0, stack)
          entity.hoverStart = 0
          Minecraft.getMinecraft.getRenderItem.renderItem(entity.getEntityItem, ItemCameraTransforms.TransformType.FIXED)
          GlStateManager.popMatrix()
        }
      }

      if (System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.world.rand.nextDouble() > 0.1) {
        RenderState.disableEntityLighting()
        RenderState.makeItBlend()

        e.renderOverlay(Textures.Block.RackDiskDriveActivity)

        RenderState.enableEntityLighting()
      }
    }
    else if (e.data != null && Servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      // Server.
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      if (e.data.getBoolean("isRunning")) {
        e.renderOverlay(Textures.Block.RackServerOn)
      }
      if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode * (e.mountable + 1))) {
        e.renderOverlay(Textures.Block.RackServerError)
      }
      if (System.currentTimeMillis() - e.data.getLong("lastFileSystemAccess") < 400 && e.rack.world.rand.nextDouble() > 0.1) {
        e.renderOverlay(Textures.Block.RackServerActivity)
      }
      if ((System.currentTimeMillis() - e.data.getLong("lastNetworkActivity") < 300 && System.currentTimeMillis() % 200 > 100) && e.data.getBoolean("isRunning")) {
        e.renderOverlay(Textures.Block.RackServerNetworkActivity)
      }

      RenderState.enableEntityLighting()
    }
    else if (e.data != null && TerminalServer == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Terminal server.
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      e.renderOverlay(Textures.Block.RackTerminalServerOn)
      val countConnected = e.data.getTagList("keys", NBT.TAG_STRING).tagCount()

      if (countConnected > 0) {
        val u0 = 7 / 16f
        val u1 = u0 + (2 * countConnected - 1) / 16f
        e.renderOverlay(Textures.Block.RackTerminalServerPresence, u0, u1)
      }

      RenderState.enableEntityLighting()
    }
  }

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.Block): Unit = {
    if (DiskDriveMountable == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Disk drive.
      e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackDiskDrive))
    }
    else if (Servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      // Server.
      e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackServer))
    }
    else if (TerminalServer == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Terminal server.
      e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackTerminalServer))
    }
  }
}
