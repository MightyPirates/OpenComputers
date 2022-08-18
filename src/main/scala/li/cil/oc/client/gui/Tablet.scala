package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

object Tablet {
  def of(id: Int, playerInventory: PlayerInventory, tablet: TabletWrapper) =
    new Tablet(new container.Tablet(container.ContainerTypes.TABLET, id, playerInventory, tablet), playerInventory, tablet.getName)
}

class Tablet(state: container.Tablet, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[container.Tablet] {

  override def lockedStack = inventoryContainer.stack
}
