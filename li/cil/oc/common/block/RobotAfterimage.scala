package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.{Settings, Blocks}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{Icon, MovingObjectPosition}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class RobotAfterimage(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "RobotAfterimage"

  override val showInItemList = false

  private var icon: Icon = _

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def icon(side: ForgeDirection) = Some(icon)

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
  }

  override def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    findMovingRobot(world, x, y, z) match {
      case Some(robot) => robot.createItemStack()
      case _ => null
    }

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def opacity(world: World, x: Int, y: Int, z: Int) = 0

  override def isNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def itemDamage = Blocks.robotProxy.blockId

  // ----------------------------------------------------------------------- //

  override def removedFromWorld(world: World, x: Int, y: Int, z: Int, blockId: Int) = {
    super.removedFromWorld(world, x, y, z, blockId)
    findMovingRobot(world, x, y, z) match {
      case Some(robot) if robot.isAnimatingMove => world.setBlockToAir(robot.x, robot.y, robot.z)
      case _ => // Probably broken by the robot we represent.
    }
  }

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) = {
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

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    findMovingRobot(world, x, y, z) match {
      case Some(robot) => Blocks.robotProxy.rightClick(world, robot.x, robot.y, robot.z, player, side, hitX, hitY, hitZ)
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
