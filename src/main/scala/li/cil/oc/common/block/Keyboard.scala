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
import net.minecraft.block.material.Material
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.state.StateContainer
import net.minecraft.world.IBlockReader
import net.minecraft.world.IWorldReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

class Keyboard(props: Properties = Properties.of(Material.STONE).strength(2, 5).noOcclusion()) extends SimpleBlock(props) {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  // ----------------------------------------------------------------------- //

  override def getBoundingBox(state: BlockState, world: IBlockReader, pos: BlockPos): AxisAlignedBB =
    world.getBlockEntity(pos) match {
      case keyboard: tileentity.Keyboard =>
        val (pitch, yaw) = (keyboard.pitch, keyboard.yaw)
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
        new AxisAlignedBB(x0, y0, z0, x1, y1, z1).move(0.5, 0.5, 0.5)
      case _ => super.getBoundingBox(state, world, pos)
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

  override def canSurvive(state: BlockState, world: IWorldReader, pos: BlockPos) = {
    world.getBlockEntity(pos) match {
      case keyboard: tileentity.Keyboard => {
        val side = keyboard.facing
        val sidePos = pos.relative(side.getOpposite)
        world.getBlockState(sidePos).isFaceSturdy(world, sidePos, side) &&
          (world.getBlockEntity(pos.relative(side.getOpposite)) match {
            case screen: tileentity.Screen => screen.facing != side
            case _ => true
          })
      }
      case _ => false
    }
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
