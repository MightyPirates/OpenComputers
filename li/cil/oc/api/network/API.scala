package li.cil.oc.api.network

import li.cil.oc.server
import net.minecraft.world.IBlockAccess

/**
 * Provides convenience methods for interacting with component networks.
 */
object API {
  /**
   * Tries to add a tile entity network node at the specified coordinates to adjacent networks.
   *
   * @param world the world the tile entity lives in.
   * @param x     the X coordinate of the tile entity.
   * @param y     the Y coordinate of the tile entity.
   * @param z     the Z coordinate of the tile entity.
   */
  def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int) =
  // TODO reflection
    server.network.Network.joinOrCreateNetwork(world, x, y, z)
}