package li.cil.oc.common.event

import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.network.Component
import li.cil.oc.server.component.UpgradeAngel
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object AngelUpgradeHandler {
  @SubscribeEvent
  def onPlaceInAir(e: RobotPlaceInAirEvent) {
    val machineNode = e.agent.machine.node
    e.setAllowed(machineNode.getReachableNodes.exists {
      case component: Component if component.canBeSeenFrom(machineNode) =>
        component.getEnvironment.isInstanceOf[UpgradeAngel]
      case _ => false
    })
  }
}
