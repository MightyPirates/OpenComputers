package li.cil.oc.common.block

import java.util

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.item.data.RaidData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.StateContainer
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeBlock

import scala.reflect.ClassTag

class Raid(props: Properties)(protected implicit val tileTag: ClassTag[tileentity.Raid])
  extends SimpleBlock(props) with IForgeBlock with traits.GUI with traits.CustomDrops[tileentity.Raid] {

  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  override protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    super.tooltipTail(stack, world, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val data = new RaidData(stack)
      for (disk <- data.disks if !disk.isEmpty) {
        tooltip.add(new StringTextComponent("- " + disk.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Raid => ContainerTypes.openRaidGui(player, te)
    case _ =>
  }

  override def newBlockEntity(world: IBlockReader) = new tileentity.Raid(tileentity.TileEntityTypes.RAID)

  // ----------------------------------------------------------------------- //

  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: World, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case raid: tileentity.Raid if raid.presence.forall(ok => ok) => 15
      case _ => 0
    }

  override protected def doCustomInit(tileEntity: tileentity.Raid, player: LivingEntity, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.world.isClientSide) {
      val data = new RaidData(stack)
      for (i <- 0 until math.min(data.disks.length, tileEntity.getContainerSize)) {
        tileEntity.setItem(i, data.disks(i))
      }
      data.label.foreach(tileEntity.label.setLabel)
      if (!data.filesystem.isEmpty) {
        tileEntity.tryCreateRaid(data.filesystem.getCompound("node").getString("address"))
        tileEntity.filesystem.foreach(_.loadData(data.filesystem))
      }
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Raid, player: PlayerEntity, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    val stack = createItemStack()
    if (tileEntity.items.exists(!_.isEmpty)) {
      val data = new RaidData()
      data.disks = tileEntity.items.clone()
      tileEntity.filesystem.foreach(_.saveData(data.filesystem))
      data.label = Option(tileEntity.label.getLabel)
      data.saveData(stack)
    }
    Block.popResource(tileEntity.world, tileEntity.getBlockPos, stack)
  }
}
