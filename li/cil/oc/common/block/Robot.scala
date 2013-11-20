package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.{GuiType, tileentity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{AxisAlignedBB, Vec3}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class Robot(val parent: SpecialDelegator) extends Computer with SpecialDelegate {
  val unlocalizedName = "Robot"

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  override def createTileEntity(world: World) = {
    moving.get match {
      case Some(robot) => Some(robot)
      case _ => Some(new tileentity.Robot(world.isRemote))
    }
  }

  // ----------------------------------------------------------------------- //

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) = {
    val bounds = parent.getCollisionBoundingBoxFromPool(world, x, y, z)
    bounds.offset(x, y, z)
    if (bounds.isVecInside(origin)) null
    else super.collisionRayTrace(world, x, y, z, origin, direction)
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) {
    world.getBlockTileEntity(x, y, z) match {
      case robot: tileentity.Robot =>
        val bounds = AxisAlignedBB.getBoundingBox(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          bounds.offset(
            -robot.moveDirection.offsetX * remaining,
            -robot.moveDirection.offsetY * remaining,
            -robot.moveDirection.offsetZ * remaining)
        }
        parent.setBlockBounds(bounds)
      case _ => super.setBlockBoundsBasedOnState(world, x, y, z)
    }
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Robot.id, world, x, y, z)
      }
      true
    }
    else false
  }

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int) {
    if (moving.get.isEmpty) {
      super.onBlockPreDestroy(world, x, y, z)
    }
  }
}
