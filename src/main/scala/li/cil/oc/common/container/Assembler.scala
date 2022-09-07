package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.common.tileentity
import net.minecraft.item.ItemStack
import net.minecraft.inventory.container.ContainerType
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Assembler(selfType: ContainerType[_ <: Assembler], id: Int, playerInventory: PlayerInventory, val assembler: IInventory)
  extends Player(selfType, id, playerInventory, assembler) {

  // Computer case.
  {
    val index = slots.size
    addSlot(new StaticComponentSlot(this, otherInventory, index, 12, 12, "template", common.Tier.Any) {
      @OnlyIn(Dist.CLIENT) override
      def isActive = !isAssembling && super.isActive

      override def mayPlace(stack: ItemStack): Boolean = {
        if (!container.canPlaceItem(getSlotIndex, stack)) return false
        if (!isActive) return false
        AssemblerTemplates.select(stack).isDefined
      }

      override def getBackgroundLocation = if (isAssembling) Textures.Icons.get(common.Tier.None) else super.getBackgroundLocation
    })
  }

  private def slotInfo(slot: DynamicComponentSlot) = {
    AssemblerTemplates.select(getSlot(0).getItem) match {
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

  override def addSlotToContainer(x: Int, y: Int, info: DynamicComponentSlot => InventorySlot) {
    val index = slots.size
    addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, info, () => common.Tier.One) {
      override def mayPlace(stack: ItemStack): Boolean = {
        if (!super.mayPlace(stack)) return false
        AssemblerTemplates.select(getSlot(0).getItem) match {
          case Some(template) =>
            val index = getSlotIndex
            val tplSlot =
              if ((1 until 4) contains index) template.containerSlots(index - 1)
              else if ((4 until 13) contains index) template.upgradeSlots(index - 4)
              else if ((13 until 21) contains index) template.componentSlots(index - 13)
              else AssemblerTemplates.NoSlot
            tplSlot.validate(assembler, getSlotIndex, stack)
          case _ => false
        }
      }
    })
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

  def assemblyRemainingTime = synchronizedData.getInt("assemblyRemainingTime")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    assembler match {
      case te: tileentity.Assembler => {
        synchronizedData.putBoolean("isAssembling", te.isAssembling)
        synchronizedData.putDouble("assemblyProgress", te.progress)
        synchronizedData.putInt("assemblyRemainingTime", te.timeRemaining)
      }
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
