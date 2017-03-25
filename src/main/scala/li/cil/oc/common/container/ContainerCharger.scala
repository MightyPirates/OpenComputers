package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class ContainerCharger(playerInventory: InventoryPlayer, charger: tileentity.TileEntityCharger) extends AbstractContainerPlayer(playerInventory, charger) {
  addSlotToContainer(80, 35, "tablet")
  addPlayerInventorySlots(8, 84)
}
