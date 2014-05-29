package li.cil.oc.common.container

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import li.cil.oc.common.tileentity
import li.cil.oc.common.InventorySlots.Tier
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.IInventory

class Robot(playerInventory: InventoryPlayer, robot: tileentity.Robot) extends Player(playerInventory, robot) {
  addSlotToContainer(170 + 0 * slotSize, 218, api.driver.Slot.Tool)
  addSlotToContainer(170 + 1 * slotSize, 218, robot.containerSlotType(1), robot.containerSlotTier(1))
  addSlotToContainer(170 + 2 * slotSize, 218, robot.containerSlotType(2), robot.containerSlotTier(2))
  addSlotToContainer(170 + 3 * slotSize, 218, robot.containerSlotType(3), robot.containerSlotTier(3))

  for (i <- 0 to 3) {
    val y = 142 + i * slotSize
    for (j <- 0 to 3) {
      val x = 170 + j * slotSize
      addSlotToContainer(new InventorySlot(this, otherInventory, inventorySlots.size, x, y))
    }
  }
  for (i <- 16 until 64) {
    addSlotToContainer(new InventorySlot(this, otherInventory, inventorySlots.size, -10000, -10000))
  }

  addPlayerInventorySlots(6, 160)

  private var lastSentBuffer = -1

  private var lastSentBufferSize = -1

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      robot.globalBuffer = value
    }

    if (id == 1) {
      robot.globalBufferSize = value
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) {
      val currentBuffer = robot.globalBuffer.toInt
      if (currentBuffer != lastSentBuffer) {
        lastSentBuffer = currentBuffer
        sendProgressBarUpdate(0, lastSentBuffer)
      }

      val currentBufferSize = robot.globalBufferSize.toInt
      if (currentBufferSize != lastSentBufferSize) {
        lastSentBufferSize = currentBufferSize
        sendProgressBarUpdate(1, lastSentBufferSize)
      }
    }
  }

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && robot.canInteract(player.getCommandSenderName)

  class InventorySlot(container: Player, inventory: IInventory, index: Int, x: Int, y: Int) extends StaticComponentSlot(container, inventory, index, x, y, api.driver.Slot.None, Tier.Any) {
    def isValid = robot.isInventorySlot(getSlotIndex)

    @SideOnly(Side.CLIENT)
    override def func_111238_b() = isValid && super.func_111238_b()

    override def getBackgroundIconIndex = {
      if (isValid) super.getBackgroundIconIndex
      else Icons.get(Tier.None)
    }

    override def getStack = {
      if (isValid) super.getStack
      else null
    }
  }
}