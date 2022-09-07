package li.cil.oc.common.container

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.template.DisassemblerTemplates
import li.cil.oc.common.tileentity
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.nbt.CompoundNBT

class Disassembler(selfType: ContainerType[_ <: Disassembler], id: Int, playerInventory: PlayerInventory, val disassembler: IInventory)
  extends Player(selfType, id, playerInventory, disassembler) {

  private def allowDisassembling(stack: ItemStack) = !stack.isEmpty && (!stack.hasTag || !stack.getTag.getBoolean(Settings.namespace + "undisassemblable"))

  addSlot(new StaticComponentSlot(this, otherInventory, slots.size, 80, 35, "ocitem", Tier.Any) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (!container.canPlaceItem(getSlotIndex, stack)) return false
      if (!isActive) return false
      allowDisassembling(stack) &&
        (((Settings.get.disassembleAllTheThings || api.Items.get(stack) != null) &&
            ItemUtils.getIngredients(playerInventory.player.level.getRecipeManager, stack).nonEmpty) ||
          DisassemblerTemplates.select(stack).isDefined)
    }
  })
  addPlayerInventorySlots(8, 84)

  def disassemblyProgress = synchronizedData.getDouble("disassemblyProgress")

  override protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    disassembler match {
      case te: tileentity.Disassembler => synchronizedData.putDouble("disassemblyProgress", te.progress)
      case _ =>
    }
    super.detectCustomDataChanges(nbt)
  }
}
