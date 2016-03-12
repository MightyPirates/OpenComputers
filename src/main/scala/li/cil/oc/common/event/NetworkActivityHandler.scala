package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.NetworkActivityEvent
import li.cil.oc.api.internal.Rack
import li.cil.oc.common.tileentity.Case
import li.cil.oc.server.component.Server

object NetworkActivityHandler {
  @SubscribeEvent
  def onNetworkActivity(e: NetworkActivityEvent.Server) {
    e.getTileEntity match {
      case t: Rack =>
        for (slot <- 0 until t.getSizeInventory) {
          t.getMountable(slot) match {
            case server: Server =>
              val containsNode = server.componentSlot(e.getNode.address) >= 0
              if (containsNode) {
                server.lastNetworkActivity = System.currentTimeMillis()
                t.markChanged(slot)
              }
            case _ =>
          }
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onNetworkActivity(e: NetworkActivityEvent.Client) {
    e.getTileEntity match {
      case t: Case => t.lastNetworkActivity = System.currentTimeMillis();
      case _ =>
    }
  }
}
