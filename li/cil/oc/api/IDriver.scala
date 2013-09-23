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
 * Do not implement this interface directly; use the `IItemDriver` and
 * `IBlockDriver` interfaces for the respective component types.
 */
trait IDriver {
  /**
   * Some initialization code that is run when the driver is installed.
   * <p/>
   * These will usually be some functions that generate network messages of
   * the particular signature the node of the driver handles, but may contain
   * arbitrary other functions. However, whatever you do, keep in mind that
   * only certain parts of the global namespace will be made available to the
   * computer at runtime, so it's best to keep all you declare in the driver
   * table (global variable `driver`).
   * <p/>
   * This is loaded into the Lua state and run in the global, un-sandboxed
   * environment. This means your scripts can mess things up bad, so make sure
   * you know what you're doing and exposing.
   * <p/>
   * This can be `None` to do nothing. Otherwise this is expected to be valid
   * Lua code (it is simply loaded via <code>load()</code> and then executed).
   * <p/>
   * The stream has to be recreated each time this is called. Normally you will
   * return something along the lines of
   * `Mod.class.getResourceAsStream("/assets/yourmod/lua/ocapi.lua")`
   * from this method. If you wish to hard-code the returned script, you can use
   * `new ByteArrayInputStream(yourScript.getBytes())` instead.
   * <p/>
   * IMPORTANT: Note that the stream will automatically closed.
   *
   * @return the Lua code to run when a computer is started up.
   */
  def api: Option[InputStream] = None
}