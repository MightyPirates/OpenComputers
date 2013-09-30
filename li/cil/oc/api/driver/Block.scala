package li.cil.oc.api.driver

import li.cil.oc.api.network.Node
import net.minecraft.world.World
import li.cil.oc.api.Driver

/**
 * Interface for block component drivers.
 * <p/>
 * This driver type is used for components that are blocks, i.e. that can be
 * placed in the world, but cannot be modified to or don't want to have their
 * `TileEntities` implement `network.Node`.
 * <p/>
 * A block driver is used by proxy blocks to check its neighbors and whether
 * those neighbors should be treated as components or not.
 * <p/>
 * Note that it is possible to write one driver that supports as many different
 * blocks as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular.
 */
trait Block extends Driver {
  /**
   * Used to determine the block types this driver handles.
   * <p/>
   * This is used to determine which driver to use for a block placed next to a
   * computer. Note that the return value should not change over time; if it
   * does, though, an already installed component will not be ejected, since
   * this value is only checked when adding components.
   *
   * @param world the world in which the block to check lives.
   * @param x     the X coordinate of the block to check.
   * @param y     the Y coordinate of the block to check.
   * @param z     the Z coordinate of the block to check.
   * @return true if the block is supported; false otherwise.
   */
  def worksWith(world: World, x: Int, y: Int, z: Int): Boolean

  /**
   * Get a reference to the network node wrapping the specified block.
   * <p/>
   * This is used to connect the component to the component network when it is
   * detected next to a proxy. Components that are not part of the component
   * network probably don't make much sense (can't think of any uses at this
   * time), but you may still opt to not implement this.
   *
   * @param world the world in which the block to get the node for lives.
   * @param x     the X coordinate of the block to get the node for.
   * @param y     the Y coordinate of the block to get the node for.
   * @param z     the Z coordinate of the block to get the node for.
   * @return the network node for the block at that location.
   */
  def node(world: World, x: Int, y: Int, z: Int): Node =
    world.getBlockTileEntity(x, y, z).asInstanceOf[Node]
}