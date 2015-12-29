package li.cil.oc.common.block

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.Rarity
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util._
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class RobotAfterimage extends SimpleBlock {
  setLightOpacity(0)
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube = false

  override def isFullCube = false

  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def getPickBlock(target: MovingObjectPosition, world: World, pos: BlockPos) =
    findMovingRobot(world, pos) match {
      case Some(robot) => robot.info.createItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(worldIn: World, meta: Int) = null

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = {
    val data = new RobotData(stack)
    Rarity.byTier(data.tier)
  }

  override def isAir(world: IBlockAccess, pos: BlockPos) = true

  // ----------------------------------------------------------------------- //

  override def onBlockAdded(world: World, pos: BlockPos, state: IBlockState) {
    world.scheduleUpdate(pos, this, math.max((Settings.get.moveDelay * 20).toInt, 1) - 1)
  }

  override def updateTick(world: World, pos: BlockPos, state: IBlockState, rand: Random) {
    world.setBlockToAir(pos)
  }

  override def removedByPlayer(world: World, pos: BlockPos, player: EntityPlayer, willHarvest: Boolean) = {
    findMovingRobot(world, pos) match {
      case Some(robot) if robot.isAnimatingMove && robot.moveFrom.contains(pos) =>
        robot.proxy.getBlockType.removedByPlayer(world, pos, player, false)
      case _ => super.removedByPlayer(world, pos, player, willHarvest) // Probably broken by the robot we represent.
    }
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos) {
    findMovingRobot(world, pos) match {
      case Some(robot) =>
        val block = robot.getBlockType
        block.setBlockBoundsBasedOnState(world, robot.getPos)
        val delta = robot.moveFrom.fold(Vec3i.NULL_VECTOR)(vec => {
          val blockPos = robot.getPos
          new BlockPos(blockPos.getX - vec.getX, blockPos.getY - vec.getY, blockPos.getZ - vec.getZ)
        })
        setBlockBounds(new AxisAlignedBB(
          block.getBlockBoundsMinX, block.getBlockBoundsMinY, block.getBlockBoundsMinZ,
          block.getBlockBoundsMaxX, block.getBlockBoundsMaxY, block.getBlockBoundsMaxZ).
          offset(delta.getX, delta.getY, delta.getZ))
      case _ => // throw new Exception("Robot afterimage without a robot found. This is a bug!")
    }
  }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    findMovingRobot(world, pos) match {
      case Some(robot) => api.Items.get(Constants.BlockName.Robot).block.onBlockActivated(world, robot.getPos, world.getBlockState(robot.getPos), player, side, hitX, hitY, hitZ)
      case _ => world.setBlockToAir(pos)
    }
  }

  def findMovingRobot(world: IBlockAccess, pos: BlockPos): Option[tileentity.Robot] = {
    for (side <- EnumFacing.values) {
      val tpos = pos.offset(side)
      if (world match {
        case world: World => world.isBlockLoaded(tpos)
        case _ => true
      }) world.getTileEntity(tpos) match {
        case proxy: tileentity.RobotProxy if proxy.robot.moveFrom.contains(pos) => return Some(proxy.robot)
        case _ =>
      }
    }
    None
  }
}
