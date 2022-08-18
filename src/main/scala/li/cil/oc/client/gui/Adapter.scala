package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

object Adapter {
  def of(id: Int, playerInventory: PlayerInventory, adapter: tileentity.Adapter): Adapter =
    new Adapter(new container.Adapter(container.ContainerTypes.ADAPTER, id, playerInventory, adapter), playerInventory, adapter.getName)
}

class Adapter(state: container.Adapter, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {
}
