package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.{Blocks, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class RobotAfterimage(val parent: SpecialDelegator) extends SpecialDelegate {
  showInItemList = false

  private var icon: Icon = _

  // ----------------------------------------------------------------------- //

  override def rarity = EnumRarity.epic

  @SideOnly(Side.CLIENT)
  override def icon(side: ForgeDirection) = Some(icon)

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":GenericTop")
  }

  override def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    findMovingRobot(world, x, y, z) match {
      case Some(robot) => robot.info.createItemStack()
      case _ => null
    }

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def opacity(world: World, x: Int, y: Int, z: Int) = 0

  override def isAir(world: World, x: Int, y: Int, z: Int) = true

  override def isNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def itemDamage = Blocks.robotProxy.blockId

  // ----------------------------------------------------------------------- //

  override def addedToWorld(world: World, x: Int, y: Int, z: Int) {
    world.scheduleBlockUpdate(x, y, z, parent.blockID, math.max((Settings.get.moveDelay * 20).toInt, 1) - 1)
  }

  override def update(world: World, x: Int, y: Int, z: Int) {
    parent.subBlock(world, x, y, z) match {
      case Some(_: RobotAfterimage) => world.setBlockToAir(x, y, z)
      case _ =>
    }
  }

  override def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = {
    findMovingRobot(world, x, y, z) match {
      case Some(robot) if robot.isAnimatingMove &&
        robot.moveFromX == x &&
        robot.moveFromY == y &&
        robot.moveFromZ == z =>
        robot.proxy.getBlockType.removeBlockByPlayer(world, player, robot.x, robot.y, robot.z)
      case _ => super.removedByEntity(world, x, y, z, player) // Probably broken by the robot we represent.
    }
  }

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    findMovingRobot(world, x, y, z) match {
      case Some(robot) =>
        val block = robot.getBlockType
        block.setBlockBoundsBasedOnState(world, robot.x, robot.y, robot.z)
        val dx = robot.x - robot.moveFromX
        val dy = robot.y - robot.moveFromY
        val dz = robot.z - robot.moveFromZ
        parent.setBlockBounds(
          block.getBlockBoundsMinX.toFloat + dx,
          block.getBlockBoundsMinY.toFloat + dy,
          block.getBlockBoundsMinZ.toFloat + dz,
          block.getBlockBoundsMaxX.toFloat + dx,
          block.getBlockBoundsMaxY.toFloat + dy,
          block.getBlockBoundsMaxZ.toFloat + dz)
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
      val (tx, ty, tz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      if (!world.isAirBlock(tx, ty, tz)) world.getBlockTileEntity(tx, ty, tz) match {
        case proxy: tileentity.RobotProxy if proxy.robot.moveFromX == x && proxy.robot.moveFromY == y && proxy.robot.moveFromZ == z => return Some(proxy.robot)
        case _ =>
      }
    }
    None
  }
}
