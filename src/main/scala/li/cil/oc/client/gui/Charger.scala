package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

object Charger {
  def of(id: Int, playerInventory: PlayerInventory, charger: tileentity.Charger) =
    new Charger(new container.Charger(container.ContainerTypes.CHARGER, id, playerInventory, charger), playerInventory, charger.getName)
}

class Charger(state: container.Charger, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {
}
