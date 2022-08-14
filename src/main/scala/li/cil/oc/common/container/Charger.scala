package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType

class Charger(selfType: ContainerType[_ <: Charger], id: Int, playerInventory: PlayerInventory, charger: IInventory)
  extends Player(selfType, id, playerInventory, charger) {

  addSlotToContainer(80, 35, "tablet")
  addPlayerInventorySlots(8, 84)
}
