package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.PlayerInventory

class Tablet(id: Int, playerInventory: PlayerInventory, val tablet: TabletWrapper)
  extends DynamicGuiContainer(new container.Tablet(container.ContainerTypes.TABLET, id, playerInventory, tablet),
    playerInventory, tablet.getName)
  with traits.LockedHotbar[container.Tablet] {

  override def lockedStack = tablet.stack
}
