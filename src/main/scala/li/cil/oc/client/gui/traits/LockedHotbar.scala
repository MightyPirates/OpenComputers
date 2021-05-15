package li.cil.oc.client.gui.traits

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

trait LockedHotbar extends GuiContainer {
  def lockedStack: ItemStack

  override def handleMouseClick(slot: Slot, slotId: Int, mouseButton: Int, clickType: ClickType): Unit = {
    if (slot == null || !slot.getStack.isItemEqual(lockedStack)) {
      super.handleMouseClick(slot, slotId, mouseButton, clickType)
    }
  }

  protected override def checkHotbarKeys(keyCode: Int) = false
}
