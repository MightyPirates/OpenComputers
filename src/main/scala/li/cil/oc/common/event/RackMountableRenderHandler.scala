package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.tileentity.RenderUtil
import li.cil.oc.util.RenderState
import net.minecraftforge.common.util.Constants.NBT

object RackMountableRenderHandler {
  lazy val Servers = Array(
    api.Items.get(Constants.ItemName.ServerTier1),
    api.Items.get(Constants.ItemName.ServerTier2),
    api.Items.get(Constants.ItemName.ServerTier3),
    api.Items.get(Constants.ItemName.ServerCreative)
  )

  lazy val TerminalServer = api.Items.get(Constants.ItemName.TerminalServer)

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.TileEntity): Unit = {
    if (e.data != null && Servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      // Server.
      RenderState.disableLighting()
      RenderState.makeItBlend()

      if (e.data.getBoolean("isRunning")) {
        e.renderOverlay(Textures.blockServerFrontOn)
      }
      if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode * (e.mountable + 1))) {
        e.renderOverlay(Textures.blockServerFrontError)
      }
      if (System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.world.rand.nextDouble() > 0.1) {
        e.renderOverlay(Textures.blockServerFrontActivity)
      }

      RenderState.enableLighting()
    }
    else if (e.data != null && TerminalServer == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Terminal server.
      RenderState.disableLighting()
      RenderState.makeItBlend()

      e.renderOverlay(Textures.blockTerminalServerFrontOn)
      val countConnected = e.data.getTagList("keys", NBT.TAG_STRING).tagCount()

      if (countConnected > 0) {
        val u0 = 7 / 16f
        val u1 = u0 + (2 * countConnected - 1) / 16f
        e.renderOverlay(Textures.blockTerminalServerFrontPresence, u0, u1)
      }

      RenderState.enableLighting()
    }
  }

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.Block): Unit = {
    if (Servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      // Server.
      e.setFrontTextureOverride(Textures.Rack.server)
    }
    else if (TerminalServer == api.Items.get(e.rack.getStackInSlot(e.mountable))) {
      // Terminal server.
      e.setFrontTextureOverride(Textures.Rack.terminal)
    }
  }
}
