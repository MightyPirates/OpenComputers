package li.cil.oc.common.block

import java.util
import java.util.Random

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.server.loot.LootFunctions
import li.cil.oc.util.Tooltip
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootContext
import net.minecraft.loot.LootParameters
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.extensions.IForgeBlock

import scala.collection.convert.ImplicitConversionsToJava._
import scala.reflect.ClassTag

class Print(props: Properties) extends RedstoneAware(props) with IForgeBlock {
  @Deprecated
  override def propagatesSkylightDown(state: BlockState, world: IBlockReader, pos: BlockPos) = false

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) = {
    super.tooltipBody(stack, world, tooltip, advanced)
    val data = new PrintData(stack)
    data.tooltip.foreach(s => tooltip.addAll(s.lines.map(new StringTextComponent(_).setStyle(Tooltip.DefaultStyle)).toIterable))
  }

  override protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) = {
    super.tooltipTail(stack, world, tooltip, advanced)
    val data = new PrintData(stack)
    if (data.isBeaconBase) {
      tooltip.add(new StringTextComponent(Localization.Tooltip.PrintBeaconBase).setStyle(Tooltip.DefaultStyle))
    }
    if (data.emitRedstone) {
      tooltip.add(new StringTextComponent(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel)).setStyle(Tooltip.DefaultStyle))
    }
    if (data.emitLight) {
      tooltip.add(new StringTextComponent(Localization.Tooltip.PrintLightValue(data.lightLevel)).setStyle(Tooltip.DefaultStyle))
    }
  }

  override def getLightValue(state: BlockState, world: IBlockReader, pos: BlockPos): Int =
    world match {
      case world: World if world.isLoaded(pos) => world.getBlockEntity(pos) match {
        case print: tileentity.Print => print.data.lightLevel
        case _ => super.getLightValue(state, world, pos)
      }
      case _ => super.getLightValue(state, world, pos)
    }

  @Deprecated
  override def getLightBlock(state: BlockState, world: IBlockReader, pos: BlockPos): Int =
    world match {
      case world: World if world.isLoaded(pos) => world.getBlockEntity(pos) match {
        case print: tileentity.Print if Settings.get.printsHaveOpacity => (print.data.opacity * 4).toInt
        case _ => super.getLightBlock(state, world, pos)
      }
      case _ => super.getLightBlock(state, world, pos)
    }

  override def getPickBlock(state: BlockState, target: RayTraceResult, world: IBlockReader, pos: BlockPos, player: PlayerEntity): ItemStack = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => print.data.createItemStack()
      case _ => ItemStack.EMPTY
    }
  }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => print.shape
      case _ => super.getShape(state, world, pos, ctx)
    }
  }

  def tickRate(world: World) = 20

  override def tick(state: BlockState, world: ServerWorld, pos: BlockPos, rand: Random): Unit = {
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case print: tileentity.Print =>
        if (print.state) print.toggleState()
      case _ =>
    }
  }

  @Deprecated
  def isBeaconBase(world: IBlockReader, pos: BlockPos, beacon: BlockPos): Boolean = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => print.data.isBeaconBase
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(worldIn: IBlockReader) = new tileentity.Print(tileentity.TileEntityTypes.PRINT)

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, trace: BlockRayTraceResult): ActionResultType = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => if (print.activate()) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
      case _ => super.use(state, world, pos, player, hand, trace)
    }
  }

  override def onRemove(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print if print.data.emitRedstone(print.state) =>
        world.updateNeighborsAt(pos, this)
        for (side <- Direction.values) {
          world.updateNeighborsAt(pos.relative(side), this)
        }
      case _ =>
    }
    super.onRemove(state, world, pos, newState, moved)
  }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack): Unit = {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case tileEntity: tileentity.Print => {
        tileEntity.data.loadData(stack)
        tileEntity.updateShape()
        tileEntity.updateRedstone()
        tileEntity.getLevel.getLightEngine.checkBlock(tileEntity.getBlockPos)
      }
      case _ =>
    }
  }

  override def getDrops(state: BlockState, ctx: LootContext.Builder): util.List[ItemStack] = {
    val newCtx = ctx.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, (c, f) => {
      c.getParamOrNull(LootParameters.BLOCK_ENTITY) match {
        case tileEntity: tileentity.Print => f.accept(tileEntity.data.createItemStack())
        case _ =>
      }
    })
    super.getDrops(state, newCtx)
  }
}
