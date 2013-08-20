package li.cil.oc.api

/**
 * This interface specifies the structure of a driver for a component.
 *
 * A driver is essentially the glue code that allows arbitrary objects to be
 * used as computer components. They specify an API that is injected into the
 * Lua state when the driver is installed, and provide general information used
 * by the computer.
 */
trait IDriver {
  /**
   * The name of the component type.
   *
   * This is used to allow computer programs to check the type of a component.
   * Components attached to a computer are identified by a number. This value
   * is returned when the type for such an ID is requested, so it should be
   * unique for each driver.
   */
  def componentName: String

  /**
   * The name of the API this component exposes to computers.
   *
   * The component may return null or an empty string if it does not wish to
   * define an API. If this is the case, we will not look for methods marked
   * with the {@link Callback} annotation.
   *
   * This should be unique for individual component types. If not, functions
   * in that API table may block another component's API from being installed
   * properly: existing entries are not overwritten.
   *
   * @return the name of the API made available to Lua.
   */
  def apiName: String

  /**
   * Some initialization code that is run when the driver is installed.
   *
   * This is run after the driver's API table has been installed. It is loaded
   * into the Lua state in a temporary environment that has access to the
   * globals table and is discarded after the script has run.
   *
   * This can be null or an empty string to do nothing. Otherwise this is
   * expected to be valid Lua code.
   *
   * @return the Lua code to run after installing the API table.
   */
  def apiCode: String

  /**
   * This is called when a component is removed from a computer.
   *
   * The component should remove all handles it currently provides to the
   * computer. For example, it should close any open files if it provides some
   * form of file system.
   *
   * @param component a handle to the component, as it was provided by the
   * driver in its {@link IBlockDriver#getComponent} or
   * {@link IItemDriver#getComponent} function.
   */
  def close(component: Object)
}