package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.{IBlockAccess, World}

class Robot(val parent: SpecialDelegator) extends Computer with SpecialDelegate {
  val unlocalizedName = "Robot"

  override def createTileEntity(world: World) = Some(new tileentity.Robot)

  // ----------------------------------------------------------------------- //

//  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) {
//    world.getBlockTileEntity(x, y, z) match {
//      case robot: tileentity.Robot => parent.setBlockBounds(robot.bounds)
//      case _ => super.setBlockBoundsBasedOnState(world, x, y, z)
//    }
//  }
}
