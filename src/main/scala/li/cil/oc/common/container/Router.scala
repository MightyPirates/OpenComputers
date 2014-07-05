package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Router(playerInventory: InventoryPlayer, router: tileentity.Router) extends Player(playerInventory, router) {
  addSlotToContainer(60, 35, api.driver.Slot.Processor)
  addSlotToContainer(95, 35, api.driver.Slot.Memory)
  addPlayerInventorySlots(8, 84)
}
