package li.cil.oc.api.driver

import net.minecraft.item.ItemStack

/**
 * Use this trait to implement components extending the memory of a computer.
 * <p/>
 * Note that the item must be installed in the actual computer's inventory to
 * work. If it is installed in an external inventory the computer will not
 * recognize the memory.
 */
trait Memory extends Item {
  /**
   * The amount of RAM this component provides, in byte.
   *
   * @param item the item to get the provided memory for.
   * @return the amount of memory the specified component provides.
   */
  def amount(item: ItemStack): Int
}
