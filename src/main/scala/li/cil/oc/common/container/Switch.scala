package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class Switch(playerInventory: InventoryPlayer, switch: tileentity.Switch) extends Player(playerInventory, switch) {
  addSlotToContainer(151, 15, api.driver.Slot.Processor)
  addSlotToContainer(151, 34, api.driver.Slot.Memory)
  addSlotToContainer(151, 53, api.driver.Slot.HardDiskDrive)
  addPlayerInventorySlots(8, 84)
}
