package li.cil.oc.common.event

import li.cil.oc.api
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.api.internal
import li.cil.oc.server.component.WirelessNetworkCard
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object WirelessNetworkCardHandler {
  @SubscribeEvent
  def onMove(e: RobotMoveEvent.Post) {
    e.agent match {
      case robot: internal.Robot =>
        val startComponents = 1 + robot.containerCount + robot.inventorySize
        for (slot <- (1 to robot.containerCount) ++ (startComponents until startComponents + robot.componentCount)) {
          robot.getComponentInSlot(slot) match {
            case card: WirelessNetworkCard => api.Network.updateWirelessNetwork(card)
            case _ =>
          }
        }
      case _ =>
    }
  }
}
