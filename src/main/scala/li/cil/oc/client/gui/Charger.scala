package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory

class Charger(id: Int, playerInventory: PlayerInventory, val charger: tileentity.Charger)
  extends DynamicGuiContainer(new container.Charger(id, playerInventory, charger),
    playerInventory, charger.getName) {
}
