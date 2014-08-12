package li.cil.oc.common.container

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.client.gui.Icons
import li.cil.oc.{Settings, common}
import li.cil.oc.common.{InventorySlots, tileentity}
import li.cil.oc.util.{ItemUtils, SideTracker}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot

class RobotAssembler(playerInventory: InventoryPlayer, val assembler: tileentity.RobotAssembler) extends Player(playerInventory, assembler) {
  // Computer case.
  {
    val index = inventorySlots.size
    addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, 12, 12, common.Slot.None, common.Tier.Any) {
      @SideOnly(Side.CLIENT) override
      def func_111238_b() = !isAssembling && super.func_111238_b()

      override def getBackgroundIconIndex = if (isAssembling) Icons.get(common.Tier.None) else super.getBackgroundIconIndex
    })
  }

  def caseTier = ItemUtils.caseTier(inventorySlots.get(0).asInstanceOf[Slot].getStack)

  // Component containers.
  for (i <- 0 until 3) {
    addSlotToContainer(34 + i * slotSize, 70, InventorySlots.assembler, () => caseTier)
  }

  // Components.
  for (i <- 0 until 9) {
    addSlotToContainer(34 + (i % 3) * slotSize, 12 + (i / 3) * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Cards.
  for (i <- 0 until 3) {
    addSlotToContainer(104, 12 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // CPU.
  addSlotToContainer(126, 12, InventorySlots.assembler, () => caseTier)

  // RAM.
  for (i <- 0 until 2) {
    addSlotToContainer(126, 30 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Floppy + HDDs.
  for (i <- 0 until 3) {
    addSlotToContainer(148, 12 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 110)

  var isAssembling = false
  var assemblyProgress = 0.0
  var assemblyRemainingTime = 0

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      isAssembling = value == 1
    }

    if (id == 1) {
      assemblyProgress = value / 5.0
    }

    if (id == 2) {
      assemblyRemainingTime = value
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      if (isAssembling != assembler.isAssembling) {
        isAssembling = assembler.isAssembling
        sendProgressBarUpdate(0, if (isAssembling) 1 else 0)
      }
      val timeRemaining = (assembler.requiredEnergy / Settings.get.assemblerTickAmount / 20).toInt
      if (math.abs(assembler.progress - assemblyProgress) > 0.2 || assemblyRemainingTime != timeRemaining) {
        assemblyProgress = assembler.progress
        assemblyRemainingTime = timeRemaining
        sendProgressBarUpdate(1, (assemblyProgress * 5).toInt)
        sendProgressBarUpdate(2, timeRemaining)
      }
    }
  }
}