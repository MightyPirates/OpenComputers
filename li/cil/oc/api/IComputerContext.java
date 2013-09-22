package li.cil.oc.api;

import net.minecraft.world.World;

/**
 * This interface is used to give drivers a controlled way of interacting with a
 * computer. It can be passed to driver API functions if they declare a
 * parameter of this type and is passed in the install and uninstall functions.
 */
public interface IComputerContext {
  /** The world the computer lives in. */
  World getWorld();

  /**
   * Send a signal to the computer.
   * 
   * Signals are like top level events. Signals are queued up and sequentially
   * processed by the computer. The queue has a maximum length; if reached, this
   * will return false. Signals only support simple types such as booleans,
   * numbers and strings. This is because unprocessed signals have to be saved
   * to NBT format when the game is saved.
   * 
   * Lua programs can register a function as a callback for each signal type,
   * which is the first parameter - the signal name. For example, two built-in
   * signals are "component_install" and "component_uninstall".
   * 
   * @param name
   *          the name of the signal.
   * @param args
   *          any parameters to pass along with the signal.
   */
  boolean signal(String name, Object[] args);
}