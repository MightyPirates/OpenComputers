package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Waypoint extends RedstoneAware {
  override protected def customTextures = Array(
    None,
    Some("WaypointTop"),
    Some("WaypointBack"),
    Some("WaypointFront"),
    Some("WaypointSide"),
    Some("WaypointSide")
  )

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Waypoint()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (world.isRemote) {
        player.openGui(OpenComputers, GuiType.Waypoint.id, world, x, y, z)
      } else {
        // If evaluation came here then  protective mods and plugins is allow interaction
        world.getTileEntity(x, y, z) match {
          case proxy: tileentity.Waypoint =>
            proxy.playersWhoCanEdit += player.getPersistentID
          case _ =>
        }
      }
      true
    }
    else super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
  }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case waypoint: tileentity.Waypoint =>
        ForgeDirection.VALID_DIRECTIONS.filter {
          d => d != waypoint.facing && d != waypoint.facing.getOpposite
        }
      case _ => super.getValidRotations(world, x, y, z)
    }
}
