package li.cil.oc.common.block

import java.util

import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ItemUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Microcontroller extends RedstoneAware with traits.PowerAcceptor {
  setCreativeTab(null)
  NEI.hide(this)

  // ----------------------------------------------------------------------- //

  override def getPickBlock(target: MovingObjectPosition, world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case mcu: tileentity.Microcontroller => mcu.info.copyItemStack()
      case _ => null
    }

  // Custom drop logic for NBT tagged item stack.
  override def getDrops(world: IBlockAccess, pos: BlockPos, state: IBlockState, fortune: Int) = new java.util.ArrayList[ItemStack]()

  override def onBlockHarvested(worldIn: World, pos: BlockPos, state: IBlockState, player: EntityPlayer) {}

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val info = new ItemUtils.MicrocontrollerData(stack)
      for (component <- info.components) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.caseRate(Tier.One)

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Microcontroller()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking && !Wrench.holdsApplicableWrench(player, pos)) {
      if (!world.isRemote) {
        world.getTileEntity(pos) match {
          case mcu: tileentity.Microcontroller =>
            if (mcu.machine.isRunning) mcu.machine.stop()
            else mcu.machine.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def onBlockPlacedBy(world: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack)
    if (!world.isRemote) world.getTileEntity(pos) match {
      case mcu: tileentity.Microcontroller =>
        mcu.info.load(stack)
        mcu.snooperNode.changeBuffer(mcu.info.storedEnergy - mcu.snooperNode.localBuffer)
      case _ =>
    }
  }

  override def removedByPlayer(world: World, pos: BlockPos, player: EntityPlayer, willHarvest: Boolean) = {
    if (!world.isRemote) {
      world.getTileEntity(pos) match {
        case mcu: tileentity.Microcontroller =>
          mcu.saveComponents()
          mcu.info.storedEnergy = mcu.snooperNode.localBuffer.toInt
          InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), mcu.info.createItemStack())
        case _ =>
      }
    }
    super.removedByPlayer(world, pos, player, willHarvest)
  }
}
