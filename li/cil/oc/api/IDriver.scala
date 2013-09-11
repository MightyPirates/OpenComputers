package li.cil.oc.api

import java.io.InputStream

/**
 * This interface specifies the structure of a driver for a component.
 *
 * A driver is essentially the glue code that allows arbitrary objects to be
 * used as computer components. They specify an API that is injected into the
 * Lua state when the driver is installed, and provide general information used
 * by the computer.
 *
 * Note that drivers themselves are singletons. They can define a parameter of
 * type {@see IComputerContext} in their API functions which will hold the
 * context in which they are called - essentially a representation of the
 * computer they were called form. This context can be used to get a component
 * in the computer (e.g. passed as another parameter) and to send signals to
 * the computer.
 *
 * Do not implement this interface directly; use the {@see IItemDriver} and
 * {@see IBlockDriver} interfaces for the respective component types.
 */
trait IDriver {
  /**
   * The name of the component type.
   *
   * This is used to allow computer programs to check the type of a component.
   * Components attached to a computer are identified by a number. This value
   * is returned when the type for such an ID is requested, so it should be
   * unique for each driver. For example, when a component is installed and the
   * install signal was sent, the Lua code may check the type of the installed
   * component using <code>drivers.component</code>. The returned string will
   * be this one.
   */
  def componentName: String

  /**
   * The name of the API this component exposes to computers, if any.
   *
   * The component may return null or an empty string if it does not wish to
   * define an API. If this is the case, we will not look for methods marked
   * with the {@link Callback} annotation.
   *
   * This is the name of the table made available in the global drivers table,
   * for example if this were to return 'disk', then the API will be available
   * in Lua via <code>drivers.disk</code>.
   *
   * This should be unique for individual component types. If not, functions
   * in that API table may block another component's API from being installed
   * properly: existing entries are not overwritten.
   *
   * Note that this <em>must not</em> be 'component', since that is reserved
   * for a function that returns the type of an installed component given its
   * ID.
   *
   * @return the name of the API made available to Lua.
   */
  def apiName: String = null

  /**
   * Some initialization code that is run when the driver is installed.
   *
   * This is run after the driver's API table has been installed. It is loaded
   * into the Lua state and run in the global, un-sandboxed environment. This
   * means your scripts can mess things up bad, so make sure you know what
   * you're doing and exposing.
   *
   * This can be null to do nothing. Otherwise this is expected to be valid Lua
   * code (it is simply loaded via <code>load()</code> and then executed).
   *
   * The stream has to be recreated each time this is called. Normally you will
   * return something along the lines of
   *   <code>Mod.class.getResourceAsStream("/assets/mod/lua/ocapi.lua")</code>
   * from this method. If you wish to hard-code the returned script, you can
   * use <code>new ByteArrayInputStream(yourScript.getBytes())</code> instead.
   * Note that the stream will automatically closed.
   *
   * @return the Lua code to run after installing the API table.
   */
  def apiCode: InputStream = null

  /**
   * Set the ID for a component supported by this driver.
   *
   * IDs are used to reference components installed in a computer from the Lua
   * side, so different components installed in a single computer must cannot
   * have the same ID. If a component with the same ID as an already installed
   * component should be installed in a computer it is simply ignored.
   *
   * @param component the component to set the ID for.
   * @param id the ID to set for the specified component.
   */
  def id(component: Any, id: Int)

  /**
   * Gets the ID of a component supported by this driver.
   *
   * @param component the component to get the ID for.
   * @return the ID of the specified component.
   */
  def id(component: Any): Int

  /**
   * This is called when a component is added to a computer.
   *
   * This happens if either of the following takes place:
   * - The component is an item component and added in the computer.
   * - The component is a block component and placed next to the computer.
   * - The component is already in / next to the computer and the computer was
   *   off and is now starting up again. In this case this is called before the
   *   computer thread is started.
   *
   * You can use this to initialize some internal state or send some signal(s)
   * to the computer. For example, graphics cards will scan for unbound
   * monitors and automatically bind to the first free one.
   *
   * This is called before the install signal is sent to the computer.
   *
   * @param computer the computer to which the component is being added.
   * @param component a handle to the component, as it was provided by the
   * driver in its {@link IBlockDriver#component}/{@link IItemDriver#component}
   * function.
   */
  def onInstall(computer: IComputerContext, component: Any) = {}

  /**
   * This is called when a component is removed from a computer.
   *
   * This happens if either of the following takes place:
   * - The component is an item component and removed from the computer.
   * - The component is a block component and broken or the computer is broken.
   * - The component is already in / next to the computer and the computer was
   *   on and is now shutting down.
   *
   * The component should remove all handles it currently provides to the
   * computer. For example, it should close any open files if it provides some
   * form of file system.
   *
   * This is called before the uninstall signal is sent to the computer.
   *
   * @param computer the computer from which the component is being removed.
   * @param component a handle to the component, as it was provided by the
   * driver in its {@link IBlockDriver#component}/{@link IItemDriver#component}
   * function.
   */
  def onUninstall(computer: IComputerContext, component: Any) = {}
}