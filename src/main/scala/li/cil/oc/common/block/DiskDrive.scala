package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class DiskDrive extends SimpleBlock with traits.GUI {
  override def createBlockState() = new BlockStateContainer(this, PropertyRotatable.Facing)

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Facing, EnumFacing.getHorizontal(meta))

  override def getMetaFromState(state: IBlockState): Int = state.getValue(PropertyRotatable.Facing).getHorizontalIndex

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (Mods.ComputerCraft.isModAvailable) {
      tooltip.addAll(Tooltip.get(getClass.getSimpleName + ".CC"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.DiskDrive

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.DiskDrive()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride(state: IBlockState): Boolean = true

  override def getComparatorInputOverride(state: IBlockState, world: World, pos: BlockPos): Int =
    world.getTileEntity(pos) match {
      case drive: tileentity.DiskDrive if drive.getStackInSlot(0) != null => 15
      case _ => 0
    }

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
    if (player.isSneaking) world.getTileEntity(pos) match {
      case drive: tileentity.DiskDrive =>
        val isDiskInDrive = drive.getStackInSlot(0) != null
        val isHoldingDisk = drive.isItemValidForSlot(0, heldItem)
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
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }
}
