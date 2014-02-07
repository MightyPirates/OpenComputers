package li.cil.oc.common.tileentity

import li.cil.oc.common.inventory
import li.cil.oc.api.network.Node

trait ComponentInventory extends Environment with Inventory with inventory.ComponentInventory {
  override def componentContainer = this

  override protected def isComponentSlot(slot: Int) = isServer

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      connectComponents()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      disconnectComponents()
    }
  }
}