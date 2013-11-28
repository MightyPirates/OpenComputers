package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  addSlotToContainer(178 + 0 * slotSize, 218, api.driver.Slot.Tool)
  addSlotToContainer(178 + 1 * slotSize, 218, api.driver.Slot.Card)
  addSlotToContainer(178 + 2 * slotSize, 218, api.driver.Slot.Disk)
  addSlotToContainer(178 + 3 * slotSize, 218, api.driver.Slot.Upgrade)

  for (i <- 0 to 3) {
    val y = 142 + i * slotSize
    for (j <- 0 to 3) {
      val x = 178 + j * slotSize
      addSlotToContainer(x, y)
    }
  }

  addPlayerInventorySlots(8, 160)

  var lastSentBuffer = -1

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if ((robot.globalBuffer - lastSentBuffer).abs > 1) {
      ServerPacketSender.sendPowerState(robot)
    }
  }

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.computer.canInteract(player.getCommandSenderName)
}