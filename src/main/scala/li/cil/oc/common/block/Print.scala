package li.cil.oc.common.block

import java.util
import java.util.Random

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.block.material.Material
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

import scala.collection.convert.WrapAsJava._
import scala.reflect.ClassTag

class Print(protected implicit val tileTag: ClassTag[tileentity.Print]) extends RedstoneAware(Properties.of(Material.METAL).strength(1, 5).noOcclusion())
  with traits.CustomDrops[tileentity.Print] {
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  @Deprecated
  override def propagatesSkylightDown(state: BlockState, world: IBlockReader, pos: BlockPos) = false

  // ----------------------------------------------------------------------- //

  override protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) = {
    super.tooltipBody(stack, world, tooltip, advanced)
    val data = new PrintData(stack)
    data.tooltip.foreach(s => tooltip.addAll(s.lines.map(new StringTextComponent(_)).toIterable))
  }

  override protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) = {
    super.tooltipTail(stack, world, tooltip, advanced)
    val data = new PrintData(stack)
    if (data.isBeaconBase) {
      tooltip.add(new StringTextComponent(Localization.Tooltip.PrintBeaconBase))
    }
    if (data.emitRedstone) {
      tooltip.add(new StringTextComponent(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel)))
    }
    if (data.emitLight) {
      tooltip.add(new StringTextComponent(Localization.Tooltip.PrintLightValue(data.lightLevel)))
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

  override def getBoundingBox(state: BlockState, world: IBlockReader, pos: BlockPos): AxisAlignedBB = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => print.bounds
      case _ => super.getBoundingBox(state, world, pos)
    }
  }

  override def isValidSpawn(state: BlockState, world: IBlockReader, pos: BlockPos, `type`: EntityType[_]): Boolean = true

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

  override def newBlockEntity(worldIn: IBlockReader) = new tileentity.Print()

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, trace: BlockRayTraceResult): ActionResultType = {
    world.getBlockEntity(pos) match {
      case print: tileentity.Print => if (print.activate()) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
      case _ => super.use(state, world, pos, player, hand, trace)
    }
  }

  override protected def doCustomInit(tileEntity: tileentity.Print, player: LivingEntity, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    tileEntity.data.loadData(stack)
    tileEntity.updateBounds()
    tileEntity.updateRedstone()
    tileEntity.getLevel.getLightEngine.checkBlock(tileEntity.getBlockPos)
  }

  override protected def doCustomDrops(tileEntity: tileentity.Print, player: PlayerEntity, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.isCreative) {
      InventoryUtils.spawnStackInWorld(tileEntity.position, tileEntity.data.createItemStack())
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
}
