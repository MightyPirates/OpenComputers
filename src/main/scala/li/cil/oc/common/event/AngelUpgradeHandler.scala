package li.cil.oc.common.event

import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.network.Node
import li.cil.oc.server.component.UpgradeAngel
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object AngelUpgradeHandler {
  @SubscribeEvent
  def onPlaceInAir(e: RobotPlaceInAirEvent) {
    val machineNode = e.agent.machine.node
    e.setAllowed(machineNode.reachableNodes.exists {
      case node: Node if node.canBeReachedFrom(machineNode) =>
        node.host.isInstanceOf[UpgradeAngel]
      case _ => false
    })
  }
}
