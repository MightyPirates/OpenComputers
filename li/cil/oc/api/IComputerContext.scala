package li.cil.oc.api

import scala.reflect.runtime.universe._

import net.minecraft.world.World

/**
 * This interface is used to give drivers a controlled way of interacting with
 * a computer. It can be passed to driver API functions if they declare a
 * parameter of this type and is passed in the install and uninstall functions.
 */
trait IComputerContext {
  /** The world the computer lives in. */
  def world: World

  /**
   * Send a signal to the computer.
   *
   * Signals are like top level events. Signals are queued up and sequentially
   * processed by the computer. The queue has a maximum length; if reached,
   * this will return false. Signals only support simple types such as booleans,
   * numbers and strings. This is because unprocessed signals have to be saved
   * to NBT format when the game is saved.
   *
   * Lua programs can register a function as a callback for each signal type,
   * which is the first parameter - the signal name. For example, two built-in
   * signals are "component_install" and "component_uninstall".
   *
   * @param name the name of the signal.
   * @param args any parameters to pass along with the signal.
   */
  def signal(name: String, args: Any*): Boolean

  /**
   * Gets a component with the specified ID from the computer.
   *
   * The Lua state refers to components only by their ID. They may pass this ID
   * along to a driver API function, so that it in turn may resolve it to the
   * actual component (originally retrieved by the computer via
   * {@see IItemDriver#getComponent(ItemStack)} or
   * {@see IBlockDriver#getComponent(Int, Int, Int)}).
   *
   * This will try to convert the component to the specified type and throw an
   * exception if the type does not match. It also throws an exception if there
   * is no such component.
   *
   * @param id the id of the component to get.
   */
  def component[T: TypeTag](id: Int): T
}