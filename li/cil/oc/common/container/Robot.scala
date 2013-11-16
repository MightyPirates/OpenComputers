package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  addSlotToContainer(new Slot(robot, getInventory.size, 176, 200) {
    setBackgroundIcon(Icons.get(api.driver.Slot.Tool))

    override def isItemValid(item: ItemStack) = {
      robot.isItemValidForSlot(0, item)
    }
  })

  for (i <- 0 to 1) {
    val index = getInventory.size
    addSlotToContainer(new Slot(robot, index, 194 + i * slotSize, 200) {
      setBackgroundIcon(Icons.get(api.driver.Slot.Card))

      override def isItemValid(item: ItemStack) = {
        robot.isItemValidForSlot(index, item)
      }
    })
  }

  for (i <- 0 to 2) {
    val y = 142 + i * slotSize
    for (j <- 0 to 2) {
      val x = 176 + j * slotSize
      val index = getInventory.size
      addSlotToContainer(new Slot(robot, index, x, y) {
        override def isItemValid(item: ItemStack) = {
          robot.isItemValidForSlot(index, item)
        }
      })
    }
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 142)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.isUser(player.getCommandSenderName)
}