package li.cil.oc.client.gui.traits

import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.inventory.container.ClickType
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.Slot
import net.minecraft.item.ItemStack

trait LockedHotbar[C <: Container] extends ContainerScreen[C] {
  def lockedStack: ItemStack

  override def slotClicked(slot: Slot, slotId: Int, mouseButton: Int, clickType: ClickType): Unit = {
    if (slot == null || !slot.getItem.sameItem(lockedStack)) {
      super.slotClicked(slot, slotId, mouseButton, clickType)
    }
  }
}
