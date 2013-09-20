package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class BlockScreen extends SubBlock {
  val unlocalizedName = "Screen"

  // ----------------------------------------------------------------------- //
  // Rendering stuff
  // ----------------------------------------------------------------------- //

  object Icons {
    var front: Icon = null
    var side: Icon = null
    var top: Icon = null
  }

  override def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = {
    side match {
      case ForgeDirection.SOUTH => Icons.front
      case ForgeDirection.DOWN | ForgeDirection.UP => Icons.top
      case _ => Icons.side
    }
  }

  override def registerIcons(iconRegister: IconRegister) = {
    Icons.front = iconRegister.registerIcon("opencomputers:screen_front")
    Icons.side = iconRegister.registerIcon("opencomputers:screen_side")
    Icons.top = iconRegister.registerIcon("opencomputers:screen_top")
  }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityScreen

  // ----------------------------------------------------------------------- //
  // Interaction
  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
    side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    player.openGui(OpenComputers, GuiType.Screen.id, world, x, y, z)
    true
  }

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  /** Avoid creating new arrays at the cost of this possibly getting modified. */
  private val validRotations = Array(
    ForgeDirection.DOWN,
    ForgeDirection.UP,
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}