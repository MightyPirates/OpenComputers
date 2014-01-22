package li.cil.oc.common.tileentity

import li.cil.oc.common.inventory

trait ComponentInventory extends Inventory with inventory.ComponentInventory {
  def componentContainer = this

  override protected def isComponentSlot(slot: Int) = isServer
}