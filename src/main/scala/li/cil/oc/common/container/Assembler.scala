package li.cil.oc.common.container

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.client.gui.Icons
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.InventoryPlayer

class Assembler(playerInventory: InventoryPlayer, val assembler: tileentity.Assembler) extends Player(playerInventory, assembler) {
  // Computer case.
  {
    val index = inventorySlots.size
    addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, 12, 12, "template", common.Tier.Any) {
      @SideOnly(Side.CLIENT) override
      def func_111238_b() = !isAssembling && super.func_111238_b()

      override def getBackgroundIconIndex = if (isAssembling) Icons.get(common.Tier.None) else super.getBackgroundIconIndex
    })
  }

  private def slotInfo(slot: DynamicComponentSlot) = {
    AssemblerTemplates.select(getSlot(0).getStack) match {
      case Some(template) =>
        val index = slot.getSlotIndex
        val tplSlot =
          if ((1 until 4) contains index) template.containerSlots(index - 1)
          else if ((4 until 13) contains index) template.upgradeSlots(index - 4)
          else if ((13 until 21) contains index) template.componentSlots(index - 13)
          else AssemblerTemplates.NoSlot
        new InventorySlot(tplSlot.kind, tplSlot.tier)
      case _ => new InventorySlot(common.Slot.None, common.Tier.None)
    }
  }

  // Component containers.
  for (i <- 0 until 3) {
    addSlotToContainer(34 + i * slotSize, 70, slotInfo _)
  }

  // Components.
  for (i <- 0 until 9) {
    addSlotToContainer(34 + (i % 3) * slotSize, 12 + (i / 3) * slotSize, slotInfo _)
  }

  // Cards.
  for (i <- 0 until 3) {
    addSlotToContainer(104, 12 + i * slotSize, slotInfo _)
  }

  // CPU.
  addSlotToContainer(126, 12, slotInfo _)

  // RAM.
  for (i <- 0 until 2) {
    addSlotToContainer(126, 30 + i * slotSize, slotInfo _)
  }

  // Floppy + HDDs.
  for (i <- 0 until 3) {
    addSlotToContainer(148, 12 + i * slotSize, slotInfo _)
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