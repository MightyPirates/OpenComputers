package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Adapter extends SimpleBlock {
  override protected def customTextures = Array(
    None,
    Some("AdapterTop"),
    Some("AdapterSide"),
    Some("AdapterSide"),
    Some("AdapterSide"),
    Some("AdapterSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Adapter()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Adapter.id, world, x, y, z)
      }
      true
    }
    else false
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) =
    world.getTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter => adapter.neighborChanged()
      case _ => // Ignore.
    }

  override def onNeighborChange(world: IBlockAccess, x: Int, y: Int, z: Int, tileX: Int, tileY: Int, tileZ: Int) =
    world.getTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter =>
        val (dx, dy, dz) = (tileX - x, tileY - y, tileZ - z)
        val index = 3 + dx + dy + dy + dz + dz + dz
        if (index >= 0 && index < sides.length) {
          adapter.neighborChanged(sides(index))
        }
      case _ => // Ignore.
    }

  private val sides = Array(
    ForgeDirection.NORTH,
    ForgeDirection.DOWN,
    ForgeDirection.WEST,
    ForgeDirection.UNKNOWN,
    ForgeDirection.EAST,
    ForgeDirection.UP,
    ForgeDirection.SOUTH)
}
