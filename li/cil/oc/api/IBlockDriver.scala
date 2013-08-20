package li.cil.oc.api

import net.minecraft.block.Block

/**
 * Interface for block component drivers.
 *
 * This driver type is used for components that are blocks, i.e. that can be
 * placed in the world, and particularly: next to computers. An example for
 * this are external drives, monitors and modems.
 *
 * When a block component is added next to a computer, the computer's OS will
 * be notified via a signal so that it may install the component's driver, for
 * example. After that the OS may start to interact with the component via the
 * API functions it provides.
 */
trait IBlockDriver extends IDriver {
  /**
   * The type of block this driver handles.
   *
   * When a block is added next to a computer and has this type, this driver
   * will be used for the block. The return value must not change over the
   * lifetime of this driver.
   *
   * @return the block type this driver is used for.
   */
  def blockType: Block

  /**
   * Get a reference to the actual component.
   *
   * This is used to provide context to the driver's methods, for example
   * when an API method is called this will always be passed as the first
   * parameter. It is also passed to the {@link IDriver#close} method.
   *
   * @param x the X coordinate of the block to get the component for.
   * @param y the Y coordinate of the block to get the component for.
   * @param z the Z coordinate of the block to get the component for.
   * @return the block component at that location, controlled by this driver.
   */
  def getComponent(x: Int, y: Int, z: Int): Object
}