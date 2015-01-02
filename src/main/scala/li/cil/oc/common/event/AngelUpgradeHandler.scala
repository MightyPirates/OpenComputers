package li.cil.oc.common.event

import li.cil.oc.api
import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.internal
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AngelUpgradeHandler {
  @SubscribeEvent
  def onPlaceInAir(e: RobotPlaceInAirEvent) {
    e.agent match {
      case robot: internal.Robot =>
        val startComponents = 1 + robot.containerCount + robot.inventorySize
        e.setAllowed(((1 to robot.containerCount) ++ (startComponents until startComponents + robot.componentCount)).
          exists(slot => api.Items.get(robot.getStackInSlot(slot)) == api.Items.get("angelUpgrade")))
      case _ =>
    }
  }
}
