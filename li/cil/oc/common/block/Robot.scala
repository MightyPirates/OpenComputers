package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.{GuiType, tileentity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class Robot(val parent: SpecialDelegator) extends Computer with SpecialDelegate {
  val unlocalizedName = "Robot"

  override def createTileEntity(world: World) = Some(new tileentity.Robot(world.isRemote))

  // ----------------------------------------------------------------------- //

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

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

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) {
    world.getBlockTileEntity(x, y, z) match {
      case robot: tileentity.Robot => parent.setBlockBounds(0.1f, 0.1f, 0.1f, 0.9f, 0.9f, 0.9f)
      case _ => super.setBlockBoundsBasedOnState(world, x, y, z)
    }
  }
}
