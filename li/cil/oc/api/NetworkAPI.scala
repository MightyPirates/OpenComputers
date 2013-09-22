package li.cil.oc.api

import java.lang.reflect.Constructor
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.ForgeDirection

/**
 * Provides convenience methods for interacting with component networks.
 */
object NetworkAPI {
  private val networkConstructor: Constructor[_] = null

  /**
   * Tries to add a tile entity network node at the specified coordinates to adjacent networks.
   *
   * @param world the world the tile entity lives in.
   * @param x     the X coordinate of the tile entity.
   * @param y     the Y coordinate of the tile entity.
   * @param z     the Z coordinate of the tile entity.
   */
  def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int) =
    getNetworkNode(world, x, y, z) match {
      case None => // Invalid block.
      case Some(node) => {
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          getNetworkNode(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case None => // Ignore.
            case Some(neighborNode) =>
              if (neighborNode != null && neighborNode.network != null) {
                neighborNode.network.connect(neighborNode, node)
              }
          }
        }
        if (node.network == null) try {
          networkConstructor.newInstance(node)
        }
        catch {
          case _: Throwable => // Ignore.
        }
      }
    }

  private def getNetworkNode(world: IBlockAccess, x: Int, y: Int, z: Int): Option[TileEntity with INetworkNode] =
    Option(Block.blocksList(world.getBlockId(x, y, z))) match {
      case Some(block) if block.hasTileEntity(world.getBlockMetadata(x, y, z)) =>
        world.getBlockTileEntity(x, y, z) match {
          case tileEntity: TileEntity with INetworkNode => Some(tileEntity)
          case _ => None
        }
      case _ => None
    }
}