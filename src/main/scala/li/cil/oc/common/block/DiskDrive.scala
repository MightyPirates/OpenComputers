package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

class DiskDrive extends SimpleBlock with traits.GUI {
  override def createBlockState(): BlockState = new BlockState(this, PropertyRotatable.Facing)

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Facing, EnumFacing.getHorizontal(meta))

  override def getMetaFromState(state: IBlockState): Int = state.getValue(PropertyRotatable.Facing).getHorizontalIndex

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (Mods.ComputerCraft.isAvailable) {
      tooltip.addAll(Tooltip.get(getClass.getSimpleName + ".CC"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.DiskDrive

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.DiskDrive()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case drive: tileentity.DiskDrive if drive.getStackInSlot(0) != null => 15
      case _ => 0
    }

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
    if (player.isSneaking) world.getTileEntity(pos) match {
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
    else super.localOnBlockActivated(world, pos, player, side, hitX, hitY, hitZ)
  }
}
