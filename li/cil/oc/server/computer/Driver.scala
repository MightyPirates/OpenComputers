package li.cil.oc.server.computer

import scala.compat.Platform.EOL
import com.naef.jnlua.LuaRuntimeException
import li.cil.oc.OpenComputers
import li.cil.oc.api.{IDriver, IBlockDriver, IItemDriver}

class ItemDriver(val instance: IItemDriver) extends Driver

class BlockDriver(val instance: IBlockDriver) extends Driver

/**
 * Wrapper for external drivers.
 *
 * We create one instance per registered driver of this. It is used to inject
 * the API the driver offers into the computer, and, in particular, for
 * generating the wrappers for the API functions, which are closures with the
 * computer the API was installed into to provide context should a function
 * require it.
 */
abstract private[oc] class Driver {
  /** The actual driver as registered via the Drivers registry. */
  def instance: IDriver

  /** Installs this driver's API on the specified computer. */
  def installOn(computer: Computer) {
    // Run custom init script.
    Option(instance.getApiCode) match {
      case None => // Nothing to do.
      case Some(apiCode) =>
        val name = instance.getClass.getName
        try {
          computer.lua.load(apiCode, name, "t") // ... func
          apiCode.close()
          computer.lua.call(0, 0) // ...
        }
        catch {
          case e: LuaRuntimeException =>
            OpenComputers.log.warning(String.format(
              "Initialization code of driver %s threw an error: %s",
              name, e.getLuaStackTrace.mkString("", EOL, EOL)))
          case e: Throwable =>
            OpenComputers.log.warning(String.format(
              "Initialization code of driver %s threw an error: %s",
              name, e.getStackTraceString))
        }
    }
  }
}