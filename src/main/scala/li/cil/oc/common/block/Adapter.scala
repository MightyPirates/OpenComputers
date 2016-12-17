package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.BlockPosition
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Adapter extends SimpleBlock with traits.GUI {
  override protected def customTextures = Array(
    None,
    Some("AdapterTop"),
    Some("AdapterSide"),
    Some("AdapterSide"),
    Some("AdapterSide"),
    Some("AdapterSide")
  )

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.Adapter

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Adapter()

  // ----------------------------------------------------------------------- //

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

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (Wrench.holdsApplicableWrench(player, BlockPosition(x, y, z))) {
      val sideToToggle = if (player.isSneaking) side.getOpposite else side
      world.getTileEntity(x, y, z) match {
        case adapter: tileentity.Adapter =>
          if (!world.isRemote) {
            val oldValue = adapter.openSides(sideToToggle.ordinal())
            adapter.setSideOpen(sideToToggle, !oldValue)
          }
          true
        case _ => false
      }
    }
    else super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
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
