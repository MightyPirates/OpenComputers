package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.tileentity.RenderUtil
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants.NBT
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
          GL11.glPushMatrix()
          GL11.glScalef(1, -1, 1)
          GL11.glTranslatef(10 / 16f, -(3.5f + e.mountable * 3f) / 16f, 1 / 16f)
          GL11.glRotatef(90, -1, 0, 0)

          val brightness = e.rack.world.getLightBrightnessForSkyBlocks(BlockPosition(e.rack).offset(e.rack.facing), 0)
          OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

          // This is very 'meh', but item frames do it like this, too!
          val entity = new EntityItem(e.rack.world, 0, 0, 0, stack)
          entity.hoverStart = 0
          RenderItem.renderInFrame = true
          RenderManager.instance.renderEntityWithPosYaw(entity, 0, 0, 0, 0, 0)
          RenderItem.renderInFrame = false
          GL11.glPopMatrix()
        }
      }

      if (System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.world.rand.nextDouble() > 0.1) {
        RenderState.disableLighting()
        RenderState.makeItBlend()

        e.renderOverlay(Textures.blockRackDiskDriveActivity)

        RenderState.enableLighting()
      }
    }
    else if (e.data != null && Servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      // Server.
      RenderState.disableLighting()
      RenderState.makeItBlend()

      if (e.data.getBoolean("isRunning")) {
        e.renderOverlay(Textures.blockRackServerOn)
      }
      if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode * (e.mountable + 1))) {
        e.renderOverlay(Textures.blockRackServerError)
      }
      if (System.currentTimeMillis() - e.data.getLong("lastFileSystemAccess") < 400 && e.rack.world.rand.nextDouble() > 0.1) {
        e.renderOverlay(Textures.blockRackServerActivity)
      }
      if ((System.currentTimeMillis() - e.data.getLong("lastNetworkActivity") < 300 && System.currentTimeMillis() % 200 > 100) && e.data.getBoolean("isRunning")) {
        e.renderOverlay(Textures.blockRackServerNetworkActivity)
      }

      RenderState.enableLighting()
    }
    else if (e.data != null && TerminalServer == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Terminal server.
      RenderState.disableLighting()
      RenderState.makeItBlend()

      e.renderOverlay(Textures.blockRackTerminalServerOn)
      val countConnected = e.data.getTagList("keys", NBT.TAG_STRING).tagCount()

      if (countConnected > 0) {
        val u0 = 7 / 16f
        val u1 = u0 + (2 * countConnected - 1) / 16f
        e.renderOverlay(Textures.blockRackTerminalServerPresence, u0, u1)
      }

      RenderState.enableLighting()
    }
  }

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.Block): Unit = {
    if (DiskDriveMountable == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Disk drive.
      e.setFrontTextureOverride(Textures.Rack.diskDrive)
    }
    else if (Servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      // Server.
      e.setFrontTextureOverride(Textures.Rack.server)
    }
    else if (TerminalServer == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Terminal server.
      e.setFrontTextureOverride(Textures.Rack.terminal)
    }
  }
}
