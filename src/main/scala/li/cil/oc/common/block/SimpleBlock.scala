package li.cil.oc.common.block

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.Colored
import li.cil.oc.common.tileentity.traits.Inventory
import li.cil.oc.common.tileentity.traits.Rotatable
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.Tooltip
import net.minecraft.block.AbstractBlock.IExtendedPositionPredicate
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.block.ContainerBlock
import net.minecraft.block.material.Material
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.DyeColor
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Rarity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.IWorldReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.ToolType

import scala.collection.convert.ImplicitConversionsToScala._

abstract class SimpleBlock(props: Properties = Properties.of(Material.METAL).strength(2, 5)) extends ContainerBlock(props.isValidSpawn(new IExtendedPositionPredicate[EntityType[_]] {
  override def test(state: BlockState, world: IBlockReader, pos: BlockPos, entity: EntityType[_]) = state.getBlock.asInstanceOf[SimpleBlock].isValidSpawn(state, world, pos, entity)
})) {
  var showInItemList = true

  @Deprecated
  private var creativeTab: ItemGroup = CreativeTab

  @Deprecated
  def getCreativeTab = creativeTab

  @Deprecated
  protected def setCreativeTab(tab: ItemGroup) = creativeTab = tab

  @Deprecated
  private var unlocalizedName = super.getDescriptionId()

  @Deprecated
  private[oc] def setUnlocalizedName(name: String): Unit = unlocalizedName = name

  @Deprecated
  override def getDescriptionId = unlocalizedName

  protected val validRotations_ = Array(Direction.UP, Direction.DOWN)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def newBlockEntity(world: IBlockReader): TileEntity = null

  // ----------------------------------------------------------------------- //
  // BlockItem
  // ----------------------------------------------------------------------- //

  def rarity(stack: ItemStack) = Rarity.COMMON

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    tooltipHead(stack, world, tooltip, flag)
    tooltipBody(stack, world, tooltip, flag)
    tooltipTail(stack, world, tooltip, flag)
  }

  protected def tooltipHead(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
  }

  protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase)) {
      tooltip.add(new StringTextComponent(curr))
    }
  }

  protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
  }

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  def getFacing(world: IBlockReader, pos: BlockPos): Direction =
    world.getBlockEntity(pos) match {
      case tileEntity: Rotatable => tileEntity.facing
      case _ => Direction.SOUTH
    }

  def setFacing(world: World, pos: BlockPos, value: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: World, pos: BlockPos, value: Entity): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  def toLocal(world: IBlockReader, pos: BlockPos, value: Direction): Direction =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.toLocal(value)
      case _ => value
    }

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  @Deprecated
  def getBoundingBox(state: BlockState, world: IBlockReader, pos: BlockPos): AxisAlignedBB = {
    val shape = super.getShape(state, world, pos, ISelectionContext.empty())
    if (shape.isEmpty) shape.bounds else new AxisAlignedBB(0, 0, 0, 1, 1, 1)
  }

  @Deprecated
  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape =
    VoxelShapes.create(getBoundingBox(state, world, pos))

  override def canHarvestBlock(state: BlockState, world: IBlockReader, pos: BlockPos, player: PlayerEntity) = true

  override def getHarvestTool(state: BlockState): ToolType = null

  override def canBeReplacedByLeaves(state: BlockState, world: IWorldReader, pos: BlockPos): Boolean = false

  @Deprecated
  def isValidSpawn(state: BlockState, world: IBlockReader, pos: BlockPos, `type`: EntityType[_]): Boolean = false

  def getValidRotations(world: World, pos: BlockPos): Array[Direction] = validRotations_

  @Deprecated
  override def onRemove(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }
    super.onRemove(state, world, pos, newState, moved)
  }

  // ----------------------------------------------------------------------- //

  @Deprecated
  def rotateBlock(world: World, pos: BlockPos, axis: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: tileentity.traits.Rotatable if rotatable.rotate(axis) =>
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, trace: BlockRayTraceResult): ActionResultType = {
    val heldItem = player.getItemInHand(hand)
    world.getBlockEntity(pos) match {
      case colored: Colored if Color.isDye(heldItem) =>
        colored.setColor(Color.rgbValues(Color.dyeColor(heldItem)))
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        if (!player.isCreative && colored.consumesDye) {
          heldItem.split(1)
        }
        ActionResultType.sidedSuccess(world.isClientSide)
      case _ => {
        val loc = trace.getLocation
        if (localOnBlockActivated(world, pos, player, hand, heldItem, trace.getDirection, loc.x.toFloat, loc.y.toFloat, loc.z.toFloat))
          ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
      }
    }
  }

  def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = false
}
