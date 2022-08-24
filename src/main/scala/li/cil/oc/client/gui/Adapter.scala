package li.cil.oc.client.gui

import li.cil.oc.common.container
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

class Adapter(state: container.Adapter, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {
}
