package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory

class Charger(id: Int, playerInventory: PlayerInventory, charger: tileentity.Charger) extends Player(null, id, playerInventory, charger) {
  addSlotToContainer(80, 35, "tablet")
  addPlayerInventorySlots(8, 84)
}
