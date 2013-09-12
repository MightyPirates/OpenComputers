package li.cil.oc.api

import li.cil.oc.server.components.IComponent

import net.minecraft.world.World

/**
 * Interface for block component drivers.
 *
 * This driver type is used for components that are blocks, i.e. that can be
 * placed in the world, and particularly: next to computers. An example for
 * this are external drives, monitors and modems.
 *
 * When a block component is placed next to a computer, the list of registered
 * drivers is queried using the drivers' {@see #worksWith} functions. The first
 * driver that replies positively will be used as the component's driver and
 * the component will be installed. If no driver is found the item will be
 * ignored.
 *
 * The computer will store a list of installed components, the values of which
 * are based on what the driver returns from its {@see #component} function
 * at the point of time the component is installed.
 * If a driver's API function queries a component via the context using
 * {@see IComputerContext#component()} the returned value will be exactly that.
 *
 * Note that it is possible to write one driver that supports as many different
 * blocks as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular and the {@see IDriver#componentName} more meaningful.
 */
trait IBlockDriver extends IDriver {
  /**
   * Used to determine the block types this driver handles.
   *
   * This is used to determine which driver to use for a block placed next to a
   * computer. Note that the return value should not change over time; if it
   * does, though, an already installed component will not be ejected, since
   * this value is only checked when adding components.
   *
   * @param world the world in which the block to check lives.
   * @param x the X coordinate of the block to check.
   * @param y the Y coordinate of the block to check.
   * @param z the Z coordinate of the block to check.
   * @return true if the block is supported; false otherwise.
   */
  def worksWith(world: World, x: Int, y: Int, z: Int): Boolean

  /**
   * Get a reference to the actual component.
   *
   * This is used to provide context to the driver's methods, for example
   * when an API method is called this will always be passed as the first
   * parameter. It is also passed to the {@link IDriver#close} method.
   *
   * @param world the world in which the block to get the component for lives.
   * @param x the X coordinate of the block to get the component for.
   * @param y the Y coordinate of the block to get the component for.
   * @param z the Z coordinate of the block to get the component for.
   * @return the block component at that location, controlled by this driver.
   */
  def component(world: World, x: Int, y: Int, z: Int): Any
}