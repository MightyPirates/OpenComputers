package li.cil.oc.api

import net.minecraft.item.ItemStack

/**
 * Interface for item component drivers.
 *
 * This driver type is used for components that are items, i.e. that can be
 * inserted into computers. An example for this are internal drives, memory
 * and power supply units.
 *
 * When an item component is added to a computer, the computer's OS will be
 * notified via a signal so that it may install the component's driver, for
 * example. After that the OS may start to interact with the component via the
 * API functions it provides.
 */
trait IItemDriver extends IDriver {
  /**
   * The component type of this item component.
   *
   * This is used to determine into which slot of a computer this component may
   * go.
   *
   * @return the component type.
   */
  def componentType: ComponentType.Value

  /**
   * The type of item this driver handles.
   *
   * When an item is added into a computer and has this type, this driver will
   * be used for the block. The return value must not change over the lifetime
   * of this driver.
   *
   * @return the item type this driver is used for.
   */
  def itemType: ItemStack

  /**
   * Get a reference to the actual component.
   *
   * This is used to provide context to the driver's methods, for example
   * when an API method is called this will always be passed as the first
   * parameter. It is also passed to the {@link IDriver#close} method.
   *
   * @param item the item instance for which to get the component.
   * @return the item component for that item, controlled by this driver.
   */
  def getComponent(item: ItemStack): Object
}