package li.cil.oc.server.component

import li.cil.oc.api.Persistable
import li.cil.oc.api.network.Node

trait RedstoneSignaller extends Persistable {
  def node: Node

  def onRedstoneChanged(side: AnyRef, oldMaxValue: AnyRef, newMaxValue: AnyRef): Unit = {
    node.sendToReachable("computer.signal", "redstone_changed", side, oldMaxValue, newMaxValue)
  }
}
