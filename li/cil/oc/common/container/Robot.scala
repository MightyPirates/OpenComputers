package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  addSlotToContainer(176 + 0 * slotSize, 200, api.driver.Slot.Tool)
  addSlotToContainer(176 + 1 * slotSize, 200, api.driver.Slot.Card)
  addSlotToContainer(176 + 2 * slotSize, 200, api.driver.Slot.HardDiskDrive)

  for (i <- 0 to 2) {
    val y = 142 + i * slotSize
    for (j <- 0 to 2) {
      val x = 176 + j * slotSize
      addSlotToContainer(x, y)
    }
  }

  addPlayerInventorySlots(8, 142)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.computer.isUser(player.getCommandSenderName)
}