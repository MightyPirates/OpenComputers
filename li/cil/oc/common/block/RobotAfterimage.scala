package li.cil.oc.common.block

import li.cil.oc.Blocks
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection
import net.minecraft.util.MovingObjectPosition

class RobotAfterimage(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "RobotAfterimage"

  override val showInItemList = false

  override def pickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    findMovingRobot(world, x, y, z) match {
      case Some(robot) => robot.createItemStack()
      case _ => null
    }

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int) = {
    super.breakBlock(world, x, y, z, blockId)
    findMovingRobot(world, x, y, z) match {
      case Some(robot) if robot.isAnimatingMove => world.setBlockToAir(robot.x, robot.y, robot.z)
      case _ => // Probably broken by the robot we represent.
    }
  }

  override def damageDropped = Blocks.robotProxy.blockId

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    findMovingRobot(world, x, y, z) match {
      case Some(robot) =>
        val block = robot.getBlockType
        block.setBlockBoundsBasedOnState(world, robot.x, robot.y, robot.z)
        parent.setBlockBounds(
          block.getBlockBoundsMinX.toFloat + robot.moveDirection.offsetX,
          block.getBlockBoundsMinY.toFloat + robot.moveDirection.offsetY,
          block.getBlockBoundsMinZ.toFloat + robot.moveDirection.offsetZ,
          block.getBlockBoundsMaxX.toFloat + robot.moveDirection.offsetX,
          block.getBlockBoundsMaxY.toFloat + robot.moveDirection.offsetY,
          block.getBlockBoundsMaxZ.toFloat + robot.moveDirection.offsetZ)
      case _ => // throw new Exception("Robot afterimage without a robot found. This is a bug!")
    }
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    findMovingRobot(world, x, y, z) match {
      case Some(robot) => robot.getBlockType.onBlockActivated(world, robot.x, robot.y, robot.z, player, side.ordinal, hitX, hitY, hitZ)
      case _ => world.setBlockToAir(x, y, z)
    }
  }

  def findMovingRobot(world: IBlockAccess, x: Int, y: Int, z: Int): Option[tileentity.Robot] = {
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      val (rx, ry, rz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      world.getBlockTileEntity(rx, ry, rz) match {
        case proxy: tileentity.RobotProxy if proxy.robot.moveDirection == side => return Some(proxy.robot)
        case _ =>
      }
    }
    None
  }
}
