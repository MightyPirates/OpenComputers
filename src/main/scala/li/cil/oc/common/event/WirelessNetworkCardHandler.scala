package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.server.component.WirelessNetworkCard

object WirelessNetworkCardHandler {
  @SubscribeEvent
  def onMove(e: RobotMoveEvent.Post) {
    val startComponents = 1 + e.robot.containerCount + e.robot.inventorySize
    for (slot <- (1 to e.robot.containerCount) ++ (startComponents until startComponents + e.robot.componentCount)) {
      e.robot.getComponentInSlot(slot) match {
        case card: WirelessNetworkCard => api.Network.updateWirelessNetwork(card)
        case _ =>
      }
    }
  }
}
