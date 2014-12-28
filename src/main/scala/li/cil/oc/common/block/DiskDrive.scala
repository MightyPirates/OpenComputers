package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

class DiskDrive extends SimpleBlock with traits.Rotatable {
  override protected def setDefaultExtendedState(state: IBlockState) = setDefaultState(state)

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (Mods.ComputerCraft.isAvailable) {
      tooltip.addAll(Tooltip.get(getClass.getSimpleName + ".CC"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.DiskDrive()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getTileEntity(pos) match {
      case drive: tileentity.DiskDrive =>
        // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
        if (!player.isSneaking) {
          if (!world.isRemote) {
            player.openGui(OpenComputers, GuiType.DiskDrive.id, world, pos.getX, pos.getY, pos.getZ)
          }
          true
        }
        else {
          val isDiskInDrive = drive.getStackInSlot(0) != null
          val isHoldingDisk = drive.isItemValidForSlot(0, player.getCurrentEquippedItem)
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
        }
      case _ => false
    }
  }
}
