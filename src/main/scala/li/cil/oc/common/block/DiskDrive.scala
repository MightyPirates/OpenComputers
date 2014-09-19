package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class DiskDrive extends SimpleBlock {
  override protected def customTextures = Array(
    None,
    None,
    Some("DiskDriveSide"),
    Some("DiskDriveFront"),
    Some("DiskDriveSide"),
    Some("DiskDriveSide")
  )

  // ----------------------------------------------------------------------- //

  override def addInformation(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    super.addInformation(metadata, stack, player, tooltip, advanced)
    if (Mods.ComputerCraft.isAvailable) {
      tooltip.addAll(Tooltip.get(getUnlocalizedName + ".CC"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.DiskDrive()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive =>
        // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
        if (!player.isSneaking) {
          if (!world.isRemote) {
            player.openGui(OpenComputers, GuiType.DiskDrive.id, world, x, y, z)
          }
          true
        }
        else {
          val isDiskInDrive = drive.getStackInSlot(0) != null
          val isHoldingDisk = drive.isItemValidForSlot(0, player.getCurrentEquippedItem)
          if (isDiskInDrive) {
            if (!world.isRemote) {
              drive.dropSlot(0, 1, drive.facing)
            }
          }
          if (isHoldingDisk) {
            // Insert the disk.
            drive.setInventorySlotContents(0, player.inventory.decrStackSize(player.inventory.currentItem, 1))
          }
          isDiskInDrive || isHoldingDisk
        }
      case _ => false
    }
  }
}
