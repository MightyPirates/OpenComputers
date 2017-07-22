package li.cil.oc.client.gui.traits

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

trait LockedHotbar extends GuiContainer {
  def lockedStack: ItemStack

  protected override def handleMouseClick(slot: Slot, slotNumber: Int, button: Int, shift: Int) {
    if (slot == null || slot.getStack != lockedStack) {
      super.handleMouseClick(slot, slotNumber, button, shift)
    }
  }

  protected override def checkHotbarKeys(keyCode: Int) = false
}
