package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity.TileEntityKeyboard
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import net.minecraft.world.IBlockAccess

class BlockKeyboard(val parent: BlockMulti) extends SubBlock {
  GameRegistry.registerTileEntity(classOf[TileEntityKeyboard], "oc.keyboard")

  val unlocalizedName = "Keyboard"

  // ----------------------------------------------------------------------- //
  // INetworkBlock
  // ----------------------------------------------------------------------- //

  override def hasNode = true

  override def getNode(world: IBlockAccess, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: TileEntityKeyboard => keyboard
    }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityKeyboard

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  /** Avoid creating new arrays at the cost of this possibly getting modified. */
  private val validRotations = Array(
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}