package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory

class Adapter(id: Int, playerInventory: PlayerInventory, val adapter: tileentity.Adapter)
  extends DynamicGuiContainer(new container.Adapter(container.ContainerTypes.ADAPTER, id, playerInventory, adapter),
    playerInventory, adapter.getName) {
}
