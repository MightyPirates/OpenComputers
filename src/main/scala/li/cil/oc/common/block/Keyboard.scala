package li.cil.oc.common.block

import java.util.Random

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedEnumFacing._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.RotationHelper
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.state.StateContainer
import net.minecraft.world.IBlockReader
import net.minecraft.world.IWorldReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Keyboard(props: Properties) extends SimpleBlock(props) {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  // ----------------------------------------------------------------------- //

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = {
    val (pitch, yaw) = (state.getValue(PropertyRotatable.Pitch), state.getValue(PropertyRotatable.Yaw))
    val (forward, up) = pitch match {
      case side@(Direction.DOWN | Direction.UP) => (side, yaw)
      case _ => (yaw, Direction.UP)
    }
    val side = forward.getRotation(up)
    val sizes = Array(7f / 16f, 4f / 16f, 7f / 16f)
    val x0 = -up.getStepX * sizes(1) - side.getStepX * sizes(2) - forward.getStepX * sizes(0)
    val x1 = up.getStepX * sizes(1) + side.getStepX * sizes(2) - forward.getStepX * 0.5f
    val y0 = -up.getStepY * sizes(1) - side.getStepY * sizes(2) - forward.getStepY * sizes(0)
    val y1 = up.getStepY * sizes(1) + side.getStepY * sizes(2) - forward.getStepY * 0.5f
    val z0 = -up.getStepZ * sizes(1) - side.getStepZ * sizes(2) - forward.getStepZ * sizes(0)
    val z1 = up.getStepZ * sizes(1) + side.getStepZ * sizes(2) - forward.getStepZ * 0.5f
    VoxelShapes.box(0.5 + x0, 0.5 + y0, 0.5 + z0, 0.5 + x1, 0.5 + y1, 0.5 + z1)
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: IBlockReader) = new tileentity.Keyboard(tileentity.TileEntityTypes.KEYBOARD)

  // ----------------------------------------------------------------------- //

  override def onPlace(state: BlockState, world: World, pos: BlockPos, prevState: BlockState, moved: Boolean): Unit = {
    if (!world.isClientSide) {
      world.asInstanceOf[ServerWorld].getBlockTicks.scheduleTick(pos, this, 10)
    }
  }

  override def tick(state: BlockState, world: ServerWorld, pos: BlockPos, rand: Random) = {
    world.getBlockEntity(pos) match {
      case keyboard: tileentity.Keyboard => api.Network.joinOrCreateNetwork(keyboard)
      case _ =>
    }
    world.getBlockTicks.scheduleTick(pos, this, 10)
  }

  override def getStateForPlacement(ctx: BlockItemUseContext): BlockState = {
    val (pitch, yaw) = ctx.getClickedFace match {
      case side@(Direction.DOWN | Direction.UP) => (side, ctx.getHorizontalDirection)
      case side => (Direction.NORTH, side)
    }
    super.getStateForPlacement(ctx).setValue(PropertyRotatable.Pitch, pitch).setValue(PropertyRotatable.Yaw, yaw)
  }

  override def canSurvive(state: BlockState, world: IWorldReader, pos: BlockPos) = {
    // Check without the TE because this is called to check if the block may be placed.
    val side = state.getValue(PropertyRotatable.Pitch) match {
      case pitch@(Direction.UP | Direction.DOWN) => pitch
      case _ => state.getValue(PropertyRotatable.Yaw)
    }
    val sidePos = pos.relative(side.getOpposite)
    world.getBlockState(sidePos).isFaceSturdy(world, sidePos, side) &&
      (world.getBlockEntity(pos.relative(side.getOpposite)) match {
        case screen: tileentity.Screen => screen.facing != side
        case _ => true
      })
  }

  @Deprecated
  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit =
    if (!canSurvive(world.getBlockState(pos), world, pos)) {
      world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState)
      InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), api.Items.get(Constants.BlockName.Keyboard).createItemStack(1))
    }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) =
    adjacencyInfo(world, pos) match {
      case Some((keyboard, screen, blockPos, facing)) => screen.rightClick(world, blockPos, player, hand, heldItem, facing, 0, 0, 0, force = true)
      case _ => false
    }

  def adjacencyInfo(world: World, pos: BlockPos) =
    world.getBlockEntity(pos) match {
      case keyboard: tileentity.Keyboard =>
        val blockPos = pos.relative(keyboard.facing.getOpposite)
        world.getBlockState(blockPos).getBlock match {
          case screen: Screen => Some((keyboard, screen, blockPos, keyboard.facing.getOpposite))
          case _ =>
            // Special case #1: check for screen in front of the keyboard.
            val forward = keyboard.facing match {
              case Direction.UP | Direction.DOWN => keyboard.yaw
              case _ => Direction.UP
            }
            val blockPos = pos.relative(forward)
            world.getBlockState(blockPos).getBlock match {
              case screen: Screen => Some((keyboard, screen, blockPos, forward))
              case _ if keyboard.facing != Direction.UP && keyboard.facing != Direction.DOWN =>
                // Special case #2: check for screen below keyboards on walls.
                val blockPos = pos.relative(forward.getOpposite)
                world.getBlockState(blockPos).getBlock match {
                  case screen: Screen => Some((keyboard, screen, blockPos, forward.getOpposite))
                  case _ => None
                }
              case _ => None
            }
        }
      case _ => None
    }

  override def getValidRotations(world: World, pos: BlockPos) = null
}
