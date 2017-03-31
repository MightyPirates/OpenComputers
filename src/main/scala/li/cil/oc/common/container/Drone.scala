package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.entity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Drone(playerInventory: InventoryPlayer, drone: entity.Drone) extends Player(playerInventory, drone.mainInventory) {
  val deltaY = 0

  for (i <- 0 to 1) {
    val y = 8 + i * slotSize - deltaY
    for (j <- 0 to 3) {
      val x = 98 + j * slotSize
      addSlotToContainer(new InventorySlot(this, otherInventory, inventorySlots.size, x, y))
    }
  }

  addPlayerInventorySlots(8, 66)

  class InventorySlot(container: Player, inventory: IInventory, index: Int, x: Int, y: Int) extends StaticComponentSlot(container, inventory, index, x, y, common.Slot.Any, common.Tier.Any) {
    def isValid = (0 until drone.mainInventory.getSizeInventory).contains(getSlotIndex)

    @SideOnly(Side.CLIENT) override
    def canBeHovered = isValid && super.canBeHovered

    override def getBackgroundLocation =
      if (isValid) super.getBackgroundLocation
      else Textures.Icons.get(common.Tier.None)

    override def getStack = {
      if (isValid) super.getStack
      else ItemStack.EMPTY
    }
  }

}