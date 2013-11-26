package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  addSlotToContainer(178 + 0 * slotSize, 218, api.driver.Slot.Tool)
  addSlotToContainer(178 + 1 * slotSize, 218, api.driver.Slot.Card)
  addSlotToContainer(178 + 2 * slotSize, 218, api.driver.Slot.Disk)

  for (i <- 0 to 3) {
    val y = 142 + i * slotSize
    for (j <- 0 to 3) {
      val x = 178 + j * slotSize
      addSlotToContainer(x, y)
    }
  }

  addPlayerInventorySlots(8, 160)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.computer.canInteract(player.getCommandSenderName)
}