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
import li.cil.oc.util.Tooltip
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item // Rarity
import net.minecraft.item.ItemStack
import net.minecraft.state.StateContainer
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeBlock

import scala.reflect.ClassTag

class Microcontroller(props: Properties)(protected implicit val tileTag: ClassTag[tileentity.Microcontroller])
  extends RedstoneAware(props) with IForgeBlock with traits.PowerAcceptor with traits.StateAware with traits.CustomDrops[tileentity.Microcontroller] {

  setCreativeTab(null)
  ItemBlacklist.hide(this)

  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: BlockState, target: RayTraceResult, world: IBlockReader, pos: BlockPos, player: PlayerEntity): ItemStack =
    world.getBlockEntity(pos) match {
      case mcu: tileentity.Microcontroller => mcu.info.copyItemStack()
      case _ => ItemStack.EMPTY
    }

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    super.tooltipTail(stack, world, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val info = new MicrocontrollerData(stack)
      for (component <- info.components if !component.isEmpty) {
        tooltip.add(new StringTextComponent("- " + component.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  override def rarity(stack: ItemStack): item.Rarity = {
    val data = new MicrocontrollerData(stack)
    Rarity.byTier(data.tier)
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput: Double = Settings.get.caseRate(Tier.One)

  override def newBlockEntity(world: IBlockReader) = new tileentity.Microcontroller(tileentity.TileEntityTypes.MICROCONTROLLER)

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!Wrench.holdsApplicableWrench(player, pos)) {
      if (!player.isCrouching) {
        if (!world.isClientSide) {
          world.getBlockEntity(pos) match {
            case mcu: tileentity.Microcontroller =>
              if (mcu.machine.isRunning) mcu.machine.stop()
              else mcu.machine.start()
            case _ =>
          }
        }
        true
      }
      else if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.EEPROM)) {
        if (!world.isClientSide) {
          world.getBlockEntity(pos) match {
            case mcu: tileentity.Microcontroller =>
              val newEeprom = player.inventory.removeItem(player.inventory.selected, 1)
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

  override protected def doCustomInit(tileEntity: tileentity.Microcontroller, player: LivingEntity, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.world.isClientSide) {
      tileEntity.info.loadData(stack)
      tileEntity.snooperNode.changeBuffer(tileEntity.info.storedEnergy - tileEntity.snooperNode.localBuffer)
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Microcontroller, player: PlayerEntity, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    tileEntity.saveComponents()
    tileEntity.info.storedEnergy = tileEntity.snooperNode.localBuffer.toInt
    Block.popResource(tileEntity.world, tileEntity.getBlockPos, tileEntity.info.createItemStack())
  }
}
