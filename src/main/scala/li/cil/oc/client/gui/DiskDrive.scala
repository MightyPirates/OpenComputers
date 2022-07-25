package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.inventory.SimpleInventory
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory

class DiskDrive(id: Int, playerInventory: PlayerInventory, val drive: SimpleInventory)
  extends DynamicGuiContainer(new container.DiskDrive(id, playerInventory, drive), playerInventory, drive.getName) {
}
