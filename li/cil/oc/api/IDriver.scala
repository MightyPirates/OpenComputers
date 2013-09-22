package li.cil.oc.api

import java.io.InputStream

/**
 * This interface specifies the structure of a driver for a component.
 * <p/>
 * A driver is essentially the glue code that allows arbitrary objects to be
 * used as computer components. They specify an API that is injected into the
 * Lua state when the driver is installed, and provide general information used
 * by the computer.
 * <p/>
 * Note that drivers themselves are singletons. They can define a parameter of
 * type {@link IComputerContext} in their API functions which will hold the
 * context in which they are called - essentially a representation of the
 * computer they were called form. This context can be used to get a component
 * in the computer (e.g. passed as another parameter) and to send signals to the
 * computer.
 * <p/>
 * Do not implement this interface directly; use the {@link IItemDriver} and
 * {@link IBlockDriver} interfaces for the respective component types.
 */
trait IDriver {
  /**
   * Some initialization code that is run when the driver is installed.
   * <p/>
   * This is loaded
   * into the Lua state and run in the global, un-sandboxed environment. This
   * means your scripts can mess things up bad, so make sure you know what
   * you're doing and exposing.
   * <p/>
   * This can be null to do nothing. Otherwise this is expected to be valid Lua
   * code (it is simply loaded via <code>load()</code> and then executed).
   * <p/>
   * The stream has to be recreated each time this is called. Normally you will
   * return something along the lines of
   * <code>Mod.class.getResourceAsStream("/assets/mod/lua/ocapi.lua")</code>
   * from this method. If you wish to hard-code the returned script, you can use
   * <code>new ByteArrayInputStream(yourScript.getBytes())</code> instead. Note
   * that the stream will automatically closed.
   *
   * @return the Lua code to run after installing the API table.
   */
  def api: Option[InputStream] = None
}