package li.cil.oc.common.block

import java.util

import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.StateContainer
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

import scala.collection.convert.ImplicitConversionsToScala._

class DiskDrive extends SimpleBlock with traits.GUI {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.tooltipTail(stack, world, tooltip, flag)
    if (Mods.ComputerCraft.isModAvailable) {
      for (curr <- Tooltip.get(getClass.getSimpleName + ".CC")) tooltip.add(new StringTextComponent(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  // ----------------------------------------------------------------------- //

  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.DiskDrive => ContainerTypes.openDiskDriveGui(player, te)
    case _ =>
  }

  override def newBlockEntity(world: IBlockReader) = new tileentity.DiskDrive(tileentity.TileEntityTypes.DISK_DRIVE)

  // ----------------------------------------------------------------------- //

  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: World, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case drive: tileentity.DiskDrive if !drive.getItem(0).isEmpty => 15
      case _ => 0
    }

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
    if (player.isCrouching) world.getBlockEntity(pos) match {
      case drive: tileentity.DiskDrive =>
        val isDiskInDrive = drive.getItem(0) != null
        val isHoldingDisk = drive.canPlaceItem(0, heldItem)
        if (isDiskInDrive) {
          if (!world.isClientSide) {
            drive.dropSlot(0, 1, Option(drive.facing))
          }
        }
        if (isHoldingDisk) {
          // Insert the disk.
          drive.setItem(0, heldItem.split(1))
        }
        isDiskInDrive || isHoldingDisk
      case _ => false
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }
}
