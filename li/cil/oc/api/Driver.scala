package li.cil.oc.api

import java.io.InputStream
import li.cil.oc.api.driver.{Item, Block}

/**
 * This interface specifies the structure of a driver for a component.
 * <p/>
 * A driver is essentially the glue code that allows arbitrary objects to be
 * used as computer components. They specify an API that is injected into the
 * Lua state when the driver is installed, and provide general information used
 * by the computer.
 * <p/>
 * Do not implement this interface directly; use the `ItemDriver` and
 * `BlockDriver` interfaces for the respective component types.
 */
trait Driver {
  /**
   * The code that is run when the driver is installed.
   * <p/>
   * The code will be run in a 'privileged' sandbox, i.e the basic environment
   * is already the sandboxed environment (thus any changes made to the globals
   * table will be noticeable - keep copies of security relevant functions). In
   * addition to the sandbox it will also have access to two more functions,
   * `sendToNode` and `sendToAll`, which will send a (synchronized) message to
   * the component network the computer is attached to.
   * <p/>
   * Drivers must take all precautions to avoid these two functions "leaking"
   * into user-space; at that point the user is pretty much in "creative mode",
   * relatively speaking (i.e. the user could send arbitrary messages into the
   * network, which is really not a good idea).
   * <p/>
   * This can be `None` to do nothing. Otherwise this is expected to be valid
   * Lua code (it is loaded via <code>load()</code> and then run).
   * <p/>
   * The stream has to be recreated each time this is called. Normally you will
   * return something along the lines of
   * `Mod.class.getResourceAsStream("/assets/yourmod/ocapi.lua")`
   * from this method. If you wish to hard-code the returned script, you can
   * use `new ByteArrayInputStream(yourScript.getBytes())` instead.
   * <p/>
   * IMPORTANT: Note that the stream will automatically closed.
   *
   * @return the Lua code to run when a computer is started up.
   */
  def api: Option[InputStream] = None
}

object Driver {
  def add(driver: Block) = instance.foreach(_.add(driver))

  def add(driver: Item) = instance.foreach(_.add(driver))

  // ----------------------------------------------------------------------- //

  /** Initialized in pre-init. */
  private[oc] var instance: Option[ {
    def add(driver: Block)

    def add(driver: Item)
  }] = None
}