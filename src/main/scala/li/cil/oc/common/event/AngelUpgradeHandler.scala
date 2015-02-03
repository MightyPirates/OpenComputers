package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api
import li.cil.oc.api.event.RobotPlaceInAirEvent

object AngelUpgradeHandler {
  @SubscribeEvent
  def onPlaceInAir(e: RobotPlaceInAirEvent) {
    // TODO Generalize Agent interface for access to their components.
//    val startComponents = 1 + e.robot.containerCount + e.robot.inventorySize
//    e.setAllowed(((1 to e.robot.containerCount) ++ (startComponents until startComponents + e.robot.componentCount)).
//      exists(slot => api.Items.get(e.robot.getStackInSlot(slot)) == api.Items.get("angelUpgrade")))
  }
}
