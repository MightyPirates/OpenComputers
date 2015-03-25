package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Assembler(playerInventory: InventoryPlayer, val assembler: tileentity.Assembler) extends Player(playerInventory, assembler) {
  // Computer case.
  {
    val index = inventorySlots.size
    addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, 12, 12, "template", common.Tier.Any) {
      @SideOnly(Side.CLIENT) override
      def canBeHovered = !isAssembling && super.canBeHovered

      override def getBackgroundLocation = if (isAssembling) Textures.Icons.get(common.Tier.None) else super.getBackgroundLocation
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

  // Floppy/EEPROM + HDDs.
  for (i <- 0 until 3) {
    addSlotToContainer(148, 12 + i * slotSize, slotInfo _)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 110)

  def isAssembling = synchronizedData.getBoolean("isAssembling")

  def assemblyProgress = synchronizedData.getDouble("assemblyProgress")

  def assemblyRemainingTime = synchronizedData.getInteger("assemblyRemainingTime")

  override protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    synchronizedData.setBoolean("isAssembling", assembler.isAssembling)
    synchronizedData.setDouble("assemblyProgress", assembler.progress)
    synchronizedData.setInteger("assemblyRemainingTime", assembler.timeRemaining)
    super.detectCustomDataChanges(nbt)
  }
}
