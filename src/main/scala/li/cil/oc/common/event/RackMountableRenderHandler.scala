package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.tileentity.RenderUtil
import li.cil.oc.util.RenderState

object RackMountableRenderHandler {
  lazy val servers = Array(
    api.Items.get(Constants.ItemName.ServerTier1),
    api.Items.get(Constants.ItemName.ServerTier2),
    api.Items.get(Constants.ItemName.ServerTier3),
    api.Items.get(Constants.ItemName.ServerCreative)
  )

  @SubscribeEvent
  def onRackMountableRendering(e: RackMountableRenderEvent.TileEntity): Unit = {
    if (e.data != null && servers.contains(api.Items.get(e.rack.getStackInSlot(e.mountable)))) {
      RenderState.disableLighting()
      RenderState.makeItBlend()

      if (e.data.getBoolean("isRunning")) {
        e.renderOverlay(Textures.blockRackFrontOn)
      }
      if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode * (e.mountable + 1))) {
        e.renderOverlay(Textures.blockRackFrontError)
      }
      if (System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.world.rand.nextDouble() > 0.1) {
        e.renderOverlay(Textures.blockRackFrontActivity)
      }

      RenderState.enableLighting()
    }
  }
}
