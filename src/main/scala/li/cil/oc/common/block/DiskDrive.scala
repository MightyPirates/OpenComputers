package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class DiskDrive extends SimpleBlock with traits.GUI {
  override protected def customTextures = Array(
    None,
    None,
    Some("DiskDriveSide"),
    Some("DiskDriveFront"),
    Some("DiskDriveSide"),
    Some("DiskDriveSide")
  )

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (Mods.ComputerCraft.isAvailable) {
      tooltip.addAll(Tooltip.get(getClass.getSimpleName + ".CC"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.DiskDrive

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.DiskDrive()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, x: Int, y: Int, z: Int, side: Int) =
    world.getTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive if drive.getStackInSlot(0) != null => 15
      case _ => 0
    }

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
    if (player.isSneaking) world.getTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive =>
        val isDiskInDrive = drive.getStackInSlot(0) != null
        val isHoldingDisk = drive.isItemValidForSlot(0, player.getHeldItem)
        if (isDiskInDrive) {
          if (!world.isRemote) {
            drive.dropSlot(0, 1, Option(drive.facing))
          }
        }
        if (isHoldingDisk) {
          // Insert the disk.
          drive.setInventorySlotContents(0, player.inventory.decrStackSize(player.inventory.currentItem, 1))
        }
        isDiskInDrive || isHoldingDisk
      case _ => false
    }
    else super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
  }
}
