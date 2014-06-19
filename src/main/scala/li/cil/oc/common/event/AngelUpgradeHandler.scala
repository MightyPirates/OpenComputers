package li.cil.oc.common.event

import li.cil.oc.api
import li.cil.oc.api.event.RobotPlaceInAirEvent
import net.minecraftforge.event.ForgeSubscribe

object AngelUpgradeHandler {
  @ForgeSubscribe
  def onPlaceInAir(e: RobotPlaceInAirEvent) {
    val startComponents = 1 + e.robot.containerCount + e.robot.inventorySize
    e.setAllowed(((1 to e.robot.containerCount) ++ (startComponents until startComponents + e.robot.componentCount)).
      exists(slot => api.Items.get(e.robot.getStackInSlot(slot)) == api.Items.get("angelUpgrade")))
  }
}
