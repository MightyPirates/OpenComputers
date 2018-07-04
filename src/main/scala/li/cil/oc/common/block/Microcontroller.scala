package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.Tier
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.Rarity
import li.cil.oc.util.StackOption._
import net.minecraft.block.Block
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.World

import scala.reflect.ClassTag

class Microcontroller(protected implicit val tileTag: ClassTag[tileentity.Microcontroller]) extends RedstoneAware with traits.PowerAcceptor with traits.StateAware with traits.CustomDrops[tileentity.Microcontroller] {
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  override def createBlockState() = new BlockStateContainer(this, PropertyRotatable.Facing)

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Facing, EnumFacing.getHorizontal(meta))

  override def getMetaFromState(state: IBlockState): Int = state.getValue(PropertyRotatable.Facing).getHorizontalIndex

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: IBlockState, target: RayTraceResult, world: World, pos: BlockPos, player: EntityPlayer): ItemStack =
    world.getTileEntity(pos) match {
      case mcu: tileentity.Microcontroller => mcu.info.copyItemStack()
      case _ => ItemStack.EMPTY
    }

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, world: World, tooltip: util.List[String], advanced: ITooltipFlag) {
    super.tooltipTail(metadata, stack, world, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val info = new MicrocontrollerData(stack)
      for (component <- info.components if !component.isEmpty) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  override def rarity(stack: ItemStack): EnumRarity = {
    val data = new MicrocontrollerData(stack)
    Rarity.byTier(data.tier)
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput: Double = Settings.get.caseRate(Tier.One)

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Microcontroller()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!Wrench.holdsApplicableWrench(player, pos)) {
      if (!player.isSneaking) {
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
      else if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.EEPROM)) {
        if (!world.isRemote) {
          world.getTileEntity(pos) match {
            case mcu: tileentity.Microcontroller =>
              val newEeprom = player.inventory.decrStackSize(player.inventory.currentItem, 1)
              mcu.changeEEPROM(newEeprom) match {
                case SomeStack(oldEeprom) => InventoryUtils.addToPlayerInventory(oldEeprom, player)
                case _ =>
              }
          }
        }
        true
      }
      else false
    }
    else false
  }

  override protected def doCustomInit(tileEntity: tileentity.Microcontroller, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.world.isRemote) {
      tileEntity.info.load(stack)
      tileEntity.snooperNode.changeBuffer(tileEntity.info.storedEnergy - tileEntity.snooperNode.localBuffer)
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Microcontroller, player: EntityPlayer, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    tileEntity.saveComponents()
    tileEntity.info.storedEnergy = tileEntity.snooperNode.localBuffer.toInt
    Block.spawnAsEntity(tileEntity.world, tileEntity.getPos, tileEntity.info.createItemStack())
  }
}
