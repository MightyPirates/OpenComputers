package li.cil.oc.common.block

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.Rarity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.FluidState
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

class RobotAfterimage(props: Properties) extends SimpleBlock(props) {
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: BlockState, target: RayTraceResult, world: IBlockReader, pos: BlockPos, player: PlayerEntity): ItemStack =
    findMovingRobot(world, pos) match {
      case Some(robot) => robot.info.createItemStack()
      case _ => ItemStack.EMPTY
    }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = {
    findMovingRobot(world, pos) match {
      case Some(robot) =>
        val block = robot.getBlockState.getBlock.asInstanceOf[SimpleBlock]
        val shape = block.getShape(state, world, robot.getBlockPos, ctx)
        val delta = robot.moveFrom.fold(BlockPos.ZERO)(vec => {
          val blockPos = robot.getBlockPos
          new BlockPos(blockPos.getX - vec.getX, blockPos.getY - vec.getY, blockPos.getZ - vec.getZ)
        })
        shape.move(delta.getX, delta.getY, delta.getZ)
      case _ => super.getShape(state, world, pos, ctx)
    }
  }

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = {
    val data = new RobotData(stack)
    Rarity.byTier(data.tier)
  }

  // ----------------------------------------------------------------------- //

  override def onPlace(state: BlockState, world: World, pos: BlockPos, prevState: BlockState, moved: Boolean): Unit = {
    if (!world.isClientSide) {
      world.asInstanceOf[ServerWorld].getBlockTicks.scheduleTick(pos, this, Math.max((Settings.get.moveDelay * 20).toInt, 1) - 1)
    }
  }

  override def tick(state: BlockState, world: ServerWorld, pos: BlockPos, rand: Random) {
    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState)
  }

  override def removedByPlayer(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, willHarvest: Boolean, fluid: FluidState): Boolean = {
    findMovingRobot(world, pos) match {
      case Some(robot) if robot.isAnimatingMove && robot.moveFrom.contains(pos) =>
        robot.proxy.getBlockState.getBlock.removedByPlayer(state, world, pos, player, false, fluid)
      case _ => super.removedByPlayer(state, world, pos, player, willHarvest, fluid) // Probably broken by the robot we represent.
    }
  }

  @Deprecated
  override def use(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, trace: BlockRayTraceResult): ActionResultType = {
    findMovingRobot(world, pos) match {
      case Some(robot) => api.Items.get(Constants.BlockName.Robot).block.use(world.getBlockState(robot.getBlockPos), world, robot.getBlockPos, player, hand, trace)
      case _ => if (world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState)) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
    }
  }

  def findMovingRobot(world: IBlockReader, pos: BlockPos): Option[tileentity.Robot] = {
    for (side <- Direction.values) {
      val tpos = pos.relative(side)
      if (world match {
        case world: World => world.isLoaded(tpos)
        case _ => true
      }) world.getBlockEntity(tpos) match {
        case proxy: tileentity.RobotProxy if proxy.robot.moveFrom.contains(pos) => return Some(proxy.robot)
        case _ =>
      }
    }
    None
  }
}
