package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.inventory.SimpleInventory
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

object DiskDrive {
  def of(id: Int, playerInventory: PlayerInventory, drive: SimpleInventory) =
    new DiskDrive(new container.DiskDrive(container.ContainerTypes.DISK_DRIVE, id, playerInventory, drive), playerInventory, drive.getName)
}

class DiskDrive(state: container.DiskDrive, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {
}
