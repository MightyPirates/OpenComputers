package li.cil.oc.common.tileentity

import li.cil.oc.common.inventory
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

trait ComponentInventory extends Inventory with inventory.ComponentInventory { self: MCTileEntity =>
  def componentContainer = this

  override protected def isComponentSlot(slot: Int) = isServer
}