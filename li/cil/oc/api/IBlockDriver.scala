package li.cil.oc.api

import _root_.scala.beans.BeanProperty
import net.minecraft.world.World

/**
 * Interface for block component drivers.
 * <p/>
 * This driver type is used for components that are blocks, i.e. that can be
 * placed in the world, but cannot be modified to or don't want to have their
 * `TileEntities` implement `INetworkNode`.
 * <p/>
 * Note that it is possible to write one driver that supports as many different
 * blocks as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular.
 */
trait IBlockDriver extends IDriver {
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
   * This is used to provide context to the driver's methods, for example when
   * an API method is called this will always be passed as the first parameter.
   *
   * @param world the world in which the block to get the component for lives.
   * @param x     the X coordinate of the block to get the component for.
   * @param y     the Y coordinate of the block to get the component for.
   * @param z     the Z coordinate of the block to get the component for.
   * @return the block component at that location, controlled by this driver.
   */
  @BeanProperty
  def node(world: World, x: Int, y: Int, z: Int): INetworkNode
}