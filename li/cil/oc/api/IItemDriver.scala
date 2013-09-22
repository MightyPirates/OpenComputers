package li.cil.oc.api

import net.minecraft.item.ItemStack

/**
 * Interface for item component drivers.
 * <p/>
 * This driver type is used for components that are items, i.e. that can be
 * inserted into computers. An example for this are internal drives, memory and
 * power supply units.
 * <p/>
 * When trying to add an item to a computer the list of registered drivers is
 * queried using the drivers' `worksWith` functions. The first driver that
 * replies positively and whose check against the slot type is successful, i.e.
 * for which the `componentType` matches the slot, will be used as the
 * component's driver and the component will be installed. If no driver is found
 * the item will be rejected and cannot be installed.
 * <p/>
 * Note that it is possible to write one driver that supports as many different
 * items as you wish. I'd recommend writing one per device (type), though, to
 * keep things modular.
 */
trait IItemDriver extends IDriver {
  /**
   * Used to determine the item types this driver handles.
   * <p/>
   * This is used to determine which driver to use for an item when installed in
   * a computer. Note that the return value should not change over time; if it
   * does, though, an already installed component will not be ejected, since
   * this value is only checked when adding components.
   *
   * @param item the item to check.
   * @return true if the item is supported; false otherwise.
   */
  def worksWith(item: ItemStack): Boolean

  /**
   * The component type of the specified item this driver supports.
   * <p/>
   * This is used to determine into which slot of a computer the components this
   * driver supports may go. This will only be called if a previous call to
   * `worksWith` with the same item type returned true.
   *
   * @return the component type of the specified item.
   */
  def componentType(item: ItemStack): ComponentType.Value

  /**
   * Gets a reference to the network node interfacing the specified item.
   *
   * @param item the item instance for which to get the node.
   * @return the network node for that item.
   */
  def node(item: ItemStack): Option[INetworkNode] = None
}