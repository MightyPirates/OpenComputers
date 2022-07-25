package li.cil.oc.common.event

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.tileentity.RenderUtil
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.model.ItemCameraTransforms
import net.minecraft.entity.item.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.vector.Vector3f
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13

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
    if (e.data != null && DiskDriveMountable == api.Items.get(e.rack.getItem(e.mountable))) {
      // Disk drive.

      if (e.data.contains("disk")) {
        val stack = ItemStack.of(e.data.getCompound("disk"))
        if (!stack.isEmpty) {
          val matrix = e.stack
          matrix.pushPose()
          matrix.scale(1, -1, 1)
          matrix.translate(10 / 16f, -(3.5f + e.mountable * 3f) / 16f, -2 / 16f)
          matrix.mulPose(Vector3f.XN.rotationDegrees(90))
          matrix.scale(0.5f, 0.5f, 0.5f)

          Minecraft.getInstance.getItemRenderer.renderStatic(stack, ItemCameraTransforms.TransformType.FIXED, e.light, e.overlay, matrix, e.typeBuffer)
          matrix.popPose()
        }
      }

      if (System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.world.random.nextDouble() > 0.1) {
        RenderState.disableEntityLighting()
        RenderState.makeItBlend()

        e.renderOverlayFromAtlas(Textures.Block.RackDiskDriveActivity)

        RenderState.disableBlend()
        RenderState.enableEntityLighting()
      }
    }
    else if (e.data != null && Servers.contains(api.Items.get(e.rack.getItem(e.mountable)))) {
      // Server.
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      if (e.data.getBoolean("isRunning")) {
        e.renderOverlayFromAtlas(Textures.Block.RackServerOn)
      }
      if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode * (e.mountable + 1))) {
        e.renderOverlayFromAtlas(Textures.Block.RackServerError)
      }
      if (System.currentTimeMillis() - e.data.getLong("lastFileSystemAccess") < 400 && e.rack.world.random.nextDouble() > 0.1) {
        e.renderOverlayFromAtlas(Textures.Block.RackServerActivity)
      }
      if ((System.currentTimeMillis() - e.data.getLong("lastNetworkActivity") < 300 && System.currentTimeMillis() % 200 > 100) && e.data.getBoolean("isRunning")) {
        e.renderOverlayFromAtlas(Textures.Block.RackServerNetworkActivity)
      }

      RenderState.disableBlend()
      RenderState.enableEntityLighting()
    }
    else if (e.data != null && TerminalServer == api.Items.get(e.rack.getItem(e.mountable))) {
      // Terminal server.
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      e.renderOverlayFromAtlas(Textures.Block.RackTerminalServerOn)
      val countConnected = e.data.getList("keys", NBT.TAG_STRING).size()

      if (countConnected > 0) {
        val u0 = 7 / 16f
        val u1 = u0 + (2 * countConnected - 1) / 16f
        e.renderOverlayFromAtlas(Textures.Block.RackTerminalServerPresence, u0, u1)
      }

      RenderState.disableBlend()
      RenderState.enableEntityLighting()
    }
  }

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.Block): Unit = {
    if (DiskDriveMountable == api.Items.get(e.rack.getItem(e.mountable))) {
      // Disk drive.
      e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackDiskDrive))
    }
    else if (Servers.contains(api.Items.get(e.rack.getItem(e.mountable)))) {
      // Server.
      e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackServer))
    }
    else if (TerminalServer == api.Items.get(e.rack.getItem(e.mountable))) {
      // Terminal server.
      e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackTerminalServer))
    }
  }
}
