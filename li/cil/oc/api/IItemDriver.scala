package li.cil.oc.api

import li.cil.oc.server.components.IComponent

import net.minecraft.item.ItemStack

/**
 * Interface for item component drivers.
 *
 * This driver type is used for components that are items, i.e. that can be
 * inserted into computers. An example for this are internal drives, memory
 * and power supply units.
 *
 * When trying to add an item to a computer the list of registered drivers is
 * queried using the drivers' {@see #worksWith} functions. The first driver
 * that replies positively and whose check against the slot type is successful,
 * i.e. for which the {@see #componentType} matches the slot, will be used as
 * the component's driver and the component will be installed. If no driver is
 * found the item will be rejected and cannot be installed.
 *
 * The computer will store a list of installed components, the values of which
 * are based on what the driver returns from its {@see #component} function
 * at the point of time the component is installed.
 * If a driver's API function queries a component via the context using
 * {@see IComputerContext#component()} the returned value will be exactly that.
 *
 * Note that it is possible to write one driver that supports as many different
 * items as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular and the {@see IDriver#componentName} more meaningful.
 */
trait IItemDriver extends IDriver {
  /**
   * Used to determine the item types this driver handles.
   *
   * This is used to determine which driver to use for an item when installed
   * in a computer. Note that the return value should not change over time; if
   * it does, though, an already installed component will not be ejected, since
   * this value is only checked when adding components.
   *
   * @param item the item to check.
   * @return true if the item is supported; false otherwise.
   */
  def worksWith(item: ItemStack): Boolean

  /**
   * The component type of the specified item this driver supports.
   *
   * This is used to determine into which slot of a computer the components
   * this driver supports may go. This will only be called if a previous call
   * to {@see #worksWith} with the same item type returned true.
   *
   * @return the component type of the specified item.
   */
  def componentType(item: ItemStack): ComponentType.Value

  /**
   * Gets a reference to the actual component.
   *
   * It is called once when a component is installed in a computer. At that
   * time, the component will be assigned a unique ID by which it is referred
   * to by from the Lua side. The computer keeps track of the mapping from ID
   * to the actual component, which will be the value returned from this.
   *
   * This is used to provide context to the driver's API methods. The driver
   * may get a reference to a component via the ID passed from a Lua program,
   * and act accordingly (for that it must also have a context parameter, see
   * the general interface documentation).
   *
   * This value also passed to the driver's {@link IDriver#close} method.
   *
   * @param item the item instance for which to get the component.
   * @return the item component for that item, controlled by this driver.
   */
  def component(item: ItemStack): Any
}