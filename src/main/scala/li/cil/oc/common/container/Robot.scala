package li.cil.oc.common.container

import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.api
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.client.gui.Icons

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  addSlotToContainer(170 + 0 * slotSize, 218, api.driver.Slot.Tool)
  addSlotToContainer(170 + 1 * slotSize, 218, robot.containerSlotType(1), robot.containerSlotTier(1))
  addSlotToContainer(170 + 2 * slotSize, 218, robot.containerSlotType(2), robot.containerSlotTier(2))
  addSlotToContainer(170 + 3 * slotSize, 218, robot.containerSlotType(3), robot.containerSlotTier(3))

  for (i <- 0 to 3) {
    val y = 142 + i * slotSize
    for (j <- 0 to 3) {
      val x = 170 + j * slotSize
      val index = inventorySlots.size
      addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, x, y, api.driver.Slot.None, Tier.Any) {
        override def getBackgroundIconIndex = {
          if (!robot.isInventorySlot(this.slotNumber)) Icons.get(Tier.None)
          super.getBackgroundIconIndex
        }
      })
    }
  }

  addPlayerInventorySlots(6, 160)

  var lastSentBuffer = Double.NegativeInfinity

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) {
      if (math.abs(robot.globalBuffer - lastSentBuffer) > 1) {
        lastSentBuffer = robot.globalBuffer
        ServerPacketSender.sendPowerState(robot)
      }
    }
  }

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.canInteract(player.getCommandSenderName)
}