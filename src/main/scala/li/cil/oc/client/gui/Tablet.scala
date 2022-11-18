package li.cil.oc.client.gui

import li.cil.oc.common.container
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

class Tablet(state: container.Tablet, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[container.Tablet] {

  override def lockedStack = inventoryContainer.stack
}
