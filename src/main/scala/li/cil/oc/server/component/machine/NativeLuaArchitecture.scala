package li.cil.oc.server.component.machine

import com.google.common.base.Strings
import com.naef.jnlua._
import java.io.{IOException, FileNotFoundException}
import java.util.logging.Level
import li.cil.oc.api.machine.{LimitReachedException, ExecutionResult}
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.util.{GameTimeFormatter, LuaStateFactory}
import li.cil.oc.{api, OpenComputers, server, Settings}
import net.minecraft.nbt.NBTTagCompound
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import li.cil.oc.api.network.ComponentConnector

class NativeLuaArchitecture(machine: api.machine.Machine) extends LuaArchitecture(machine) {
  private var lua: LuaState = null

  private var kernelMemory = 0

  private val ramScale = if (LuaStateFactory.is64Bit) Settings.get.ramScaleFor64Bit else 1.0

  // ----------------------------------------------------------------------- //

  private def node = machine.node.asInstanceOf[ComponentConnector]

  private def components = machine.components

  // ----------------------------------------------------------------------- //

  override def name() = "Lua"

  override def isInitialized = kernelMemory > 0

  override def recomputeMemory() = Option(lua) match {
    case Some(l) =>
      l.setTotalMemory(Int.MaxValue)
      l.gc(LuaState.GcAction.COLLECT, 0)
      if (kernelMemory > 0) {
        l.setTotalMemory(kernelMemory + math.ceil(machine.owner.installedMemory * ramScale).toInt)
      }
    case _ =>
  }

  // ----------------------------------------------------------------------- //

  override def runSynchronized() {
    // These three asserts are all guaranteed by run().
    assert(lua.getTop == 2)
    assert(lua.isThread(1))
    assert(lua.isFunction(2))

    try {
      // Synchronized call protocol requires the called function to return
      // a table, which holds the results of the call, to be passed back
      // to the coroutine.yield() that triggered the call.
      lua.call(0, 1)
      lua.checkType(2, LuaType.TABLE)
    }
    catch {
      case _: LuaMemoryAllocationException =>
        // This can happen if we run out of memory while converting a Java
        // exception to a string (which we have to do to avoid keeping
        // userdata on the stack, which cannot be persisted).
        throw new java.lang.OutOfMemoryError("not enough memory")
    }
  }

  override def runThreaded(isSynchronizedReturn: Boolean): ExecutionResult = {
    try {
      // The kernel thread will always be at stack index one.
      assert(lua.isThread(1))

      if (Settings.get.activeGC) {
        // Help out the GC a little. The emergency GC has a few limitations
        // that will make it free less memory than doing a full step manually.
        lua.gc(LuaState.GcAction.COLLECT, 0)
      }

      // Resume the Lua state and remember the number of results we get.
      val results = if (isSynchronizedReturn) {
        // If we were doing a synchronized call, continue where we left off.
        assert(lua.getTop == 2)
        assert(lua.isTable(2))
        lua.resume(1, 1)
      }
      else {
        if (kernelMemory == 0) {
          // We're doing the initialization run.
          if (lua.resume(1, 0) > 0) {
            // We expect to get nothing here, if we do we had an error.
            0
          }
          else {
            // Run the garbage collector to get rid of stuff left behind after
            // the initialization phase to get a good estimate of the base
            // memory usage the kernel has (including libraries). We remember
            // that size to grant user-space programs a fixed base amount of
            // memory, regardless of the memory need of the underlying system
            // (which may change across releases).
            lua.gc(LuaState.GcAction.COLLECT, 0)
            kernelMemory = math.max(lua.getTotalMemory - lua.getFreeMemory, 1)
            recomputeMemory()

            // Fake zero sleep to avoid stopping if there are no signals.
            lua.pushInteger(0)
            1
          }
        }
        else machine.popSignal() match {
          case signal if signal != null =>
            lua.pushString(signal.name)
            signal.args.foreach(arg => lua.pushValue(arg))
            lua.resume(1, 1 + signal.args.length)
          case _ =>
            lua.resume(1, 0)
        }
      }

      // Check if the kernel is still alive.
      if (lua.status(1) == LuaState.YIELD) {
        // If we get one function it must be a wrapper for a synchronized
        // call. The protocol is that a closure is pushed that is then called
        // from the main server thread, and returns a table, which is in turn
        // passed to the originating coroutine.yield().
        if (results == 1 && lua.isFunction(2)) {
          new ExecutionResult.SynchronizedCall()
        }
        // Check if we are shutting down, and if so if we're rebooting. This
        // is signalled by boolean values, where `false` means shut down,
        // `true` means reboot (i.e shutdown then start again).
        else if (results == 1 && lua.isBoolean(2)) {
          new ExecutionResult.Shutdown(lua.toBoolean(2))
        }
        else {
          // If we have a single number, that's how long we may wait before
          // resuming the state again. Note that the sleep may be interrupted
          // early if a signal arrives in the meantime. If we have something
          // else we just process the next signal or wait for one.
          val ticks = if (results == 1 && lua.isNumber(2)) (lua.toNumber(2) * 20).toInt else Int.MaxValue
          lua.pop(results)
          new ExecutionResult.Sleep(ticks)
        }
      }
      // The kernel thread returned. If it threw we'd be in the catch below.
      else {
        assert(lua.isThread(1))
        // We're expecting the result of a pcall, if anything, so boolean + (result | string).
        if (!lua.isBoolean(2) || !(lua.isString(3) || lua.isNil(3))) {
          OpenComputers.log.warning("Kernel returned unexpected results.")
        }
        // The pcall *should* never return normally... but check for it nonetheless.
        if (lua.toBoolean(2)) {
          OpenComputers.log.warning("Kernel stopped unexpectedly.")
          new ExecutionResult.Shutdown(false)
        }
        else {
          lua.setTotalMemory(Int.MaxValue)
          val error = lua.toString(3)
          if (error != null) new ExecutionResult.Error(error)
          else new ExecutionResult.Error("unknown error")
        }
      }
    }
    catch {
      case e: LuaRuntimeException =>
        OpenComputers.log.warning("Kernel crashed. This is a bug!\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it")
      case e: LuaGcMetamethodException =>
        if (e.getMessage != null) new ExecutionResult.Error("kernel panic:\n" + e.getMessage)
        else new ExecutionResult.Error("kernel panic:\nerror in garbage collection metamethod")
      case e: LuaMemoryAllocationException =>
        new ExecutionResult.Error("not enough memory")
      case e: java.lang.Error if e.getMessage == "not enough memory" =>
        new ExecutionResult.Error("not enough memory")
    }
  }

  // ----------------------------------------------------------------------- //

  override def initialize(): Boolean = {
    super.initialize()

    // Creates a new state with all base libraries and the persistence library
    // loaded into it. This means the state has much more power than it
    // rightfully should have, so we sandbox it a bit in the following.
    LuaStateFactory.createState() match {
      case None =>
        lua = null
        machine.crash("native libraries not available")
        return false
      case Some(value) => lua = value
    }

    // Push a couple of functions that override original Lua API functions or
    // that add new functionality to it.
    lua.getGlobal("os")

    // Custom os.clock() implementation returning the time the computer has
    // been actively running, instead of the native library...
    lua.pushScalaFunction(lua => {
      lua.pushNumber(machine.cpuTime())
      1
    })
    lua.setField(-2, "clock")

    // Date formatting function.
    lua.pushScalaFunction(lua => {
      val format =
        if (lua.getTop > 0 && lua.isString(1)) lua.toString(1)
        else "%d/%m/%y %H:%M:%S"
      val time =
        if (lua.getTop > 1 && lua.isNumber(2)) lua.toNumber(2) * 1000 / 60 / 60
        else machine.worldTime + 6000

      val dt = GameTimeFormatter.parse(time)
      def fmt(format: String) {
        if (format == "*t") {
          lua.newTable(0, 8)
          lua.pushInteger(dt.year)
          lua.setField(-2, "year")
          lua.pushInteger(dt.month)
          lua.setField(-2, "month")
          lua.pushInteger(dt.day)
          lua.setField(-2, "day")
          lua.pushInteger(dt.hour)
          lua.setField(-2, "hour")
          lua.pushInteger(dt.minute)
          lua.setField(-2, "min")
          lua.pushInteger(dt.second)
          lua.setField(-2, "sec")
          lua.pushInteger(dt.weekDay)
          lua.setField(-2, "wday")
          lua.pushInteger(dt.yearDay)
          lua.setField(-2, "yday")
        }
        else {
          lua.pushString(GameTimeFormatter.format(format, dt))
        }
      }

      // Just ignore the allowed leading '!', Minecraft has no time zones...
      if (format.startsWith("!"))
        fmt(format.substring(1))
      else
        fmt(format)
      1
    })
    lua.setField(-2, "date")

    // Return ingame time for os.time().
    lua.pushScalaFunction(lua => {
      // Game time is in ticks, so that each day has 24000 ticks, meaning
      // one hour is game time divided by one thousand. Also, Minecraft
      // starts days at 6 o'clock, so we add those six hours. Thus:
      // timestamp = (time + 6000) * 60[kh] * 60[km] / 1000[s]
      lua.pushNumber((machine.worldTime + 6000) * 60 * 60 / 1000)
      1
    })
    lua.setField(-2, "time")

    // Pop the os table.
    lua.pop(1)

    // Computer API, stuff that kinda belongs to os, but we don't want to
    // clutter it.
    lua.newTable()

    // Allow getting the real world time for timeouts.
    lua.pushScalaFunction(lua => {
      lua.pushNumber(System.currentTimeMillis() / 1000.0)
      1
    })
    lua.setField(-2, "realTime")

    // The time the computer has been running, as opposed to the CPU time.
    lua.pushScalaFunction(lua => {
      // World time is in ticks, and each second has 20 ticks. Since we
      // want uptime() to return real seconds, though, we'll divide it
      // accordingly.
      lua.pushNumber(machine.upTime())
      1
    })
    lua.setField(-2, "uptime")

    // Allow the computer to figure out its own id in the component network.
    lua.pushScalaFunction(lua => {
      Option(node.address) match {
        case None => lua.pushNil()
        case Some(address) => lua.pushString(address)
      }
      1
    })
    lua.setField(-2, "address")

    // Are we a robot? (No this is not a CAPTCHA.)
    // TODO deprecate this
    lua.pushScalaFunction(lua => {
      lua.pushBoolean(machine.components.contains("robot"))
      1
    })
    lua.setField(-2, "isRobot")

    lua.pushScalaFunction(lua => {
      // This is *very* unlikely, but still: avoid this getting larger than
      // what we report as the total memory.
      lua.pushInteger(((lua.getFreeMemory min (lua.getTotalMemory - kernelMemory)) / ramScale).toInt)
      1
    })
    lua.setField(-2, "freeMemory")

    // Allow the system to read how much memory it uses and has available.
    lua.pushScalaFunction(lua => {
      lua.pushInteger(((lua.getTotalMemory - kernelMemory) / ramScale).toInt)
      1
    })
    lua.setField(-2, "totalMemory")

    lua.pushScalaFunction(lua => {
      lua.pushBoolean(machine.signal(lua.checkString(1), lua.toSimpleJavaObjects(2): _*))
      1
    })
    lua.setField(-2, "pushSignal")

    // And its ROM address.
    lua.pushScalaFunction(lua => {
      rom.foreach(fs => Option(fs.node.address) match {
        case None => lua.pushNil()
        case Some(address) => lua.pushString(address)
      })
      1
    })
    lua.setField(-2, "romAddress")

    // And it's /tmp address...
    lua.pushScalaFunction(lua => {
      val address = machine.tmpAddress
      if (address == null) lua.pushNil()
      else lua.pushString(address)
      1
    })
    lua.setField(-2, "tmpAddress")

    // User management.
    lua.pushScalaFunction(lua => {
      val users = machine.users
      users.foreach(lua.pushString)
      users.length
    })
    lua.setField(-2, "users")

    lua.pushScalaFunction(lua => try {
      machine.addUser(lua.checkString(1))
      lua.pushBoolean(true)
      1
    } catch {
      case e: Throwable =>
        lua.pushNil()
        lua.pushString(Option(e.getMessage).getOrElse(e.toString))
        2
    })
    lua.setField(-2, "addUser")

    lua.pushScalaFunction(lua => {
      lua.pushBoolean(machine.removeUser(lua.checkString(1)))
      1
    })
    lua.setField(-2, "removeUser")

    lua.pushScalaFunction(lua => {
      lua.pushNumber(node.globalBuffer)
      1
    })
    lua.setField(-2, "energy")

    lua.pushScalaFunction(lua => {
      lua.pushNumber(node.globalBufferSize)
      1
    })
    lua.setField(-2, "maxEnergy")

    // Set the computer table.
    lua.setGlobal("computer")

    // Until we get to ingame screens we log to Java's stdout.
    lua.pushScalaFunction(lua => {
      println((1 to lua.getTop).map(i => lua.`type`(i) match {
        case LuaType.NIL => "nil"
        case LuaType.BOOLEAN => lua.toBoolean(i)
        case LuaType.NUMBER => lua.toNumber(i)
        case LuaType.STRING => lua.toString(i)
        case LuaType.TABLE => "table"
        case LuaType.FUNCTION => "function"
        case LuaType.THREAD => "thread"
        case LuaType.LIGHTUSERDATA | LuaType.USERDATA => "userdata"
      }).mkString("  "))
      0
    })
    lua.setGlobal("print")

    // Whether bytecode may be loaded directly.
    lua.pushScalaFunction(lua => {
      lua.pushBoolean(Settings.get.allowBytecode)
      1
    })
    lua.setGlobal("allowBytecode")

    // How long programs may run without yielding before we stop them.
    lua.pushNumber(Settings.get.timeout)
    lua.setGlobal("timeout")

    // Component interaction stuff.
    lua.newTable()

    lua.pushScalaFunction(lua => components.synchronized {
      val filter = if (lua.isString(1)) Option(lua.toString(1)) else None
      lua.newTable(0, components.size)
      for ((address, name) <- components) {
        if (filter.isEmpty || name.contains(filter.get)) {
          lua.pushString(address)
          lua.pushString(name)
          lua.rawSet(-3)
        }
      }
      1
    })
    lua.setField(-2, "list")

    lua.pushScalaFunction(lua => components.synchronized {
      components.get(lua.checkString(1)) match {
        case name: String =>
          lua.pushString(name)
          1
        case _ =>
          lua.pushNil()
          lua.pushString("no such component")
          2
      }
    })
    lua.setField(-2, "type")

    lua.pushScalaFunction(lua => {
      Option(node.network.node(lua.checkString(1))) match {
        case Some(component: server.network.Component) if component.canBeSeenFrom(node) || component == node =>
          lua.newTable()
          for (method <- component.methods()) {
            lua.pushString(method)
            lua.pushBoolean(component.isDirect(method))
            lua.rawSet(-3)
          }
          1
        case _ =>
          lua.pushNil()
          lua.pushString("no such component")
          2
      }
    })
    lua.setField(-2, "methods")

    lua.pushScalaFunction(lua => {
      val address = lua.checkString(1)
      val method = lua.checkString(2)
      val args = lua.toSimpleJavaObjects(3)
      try {
        machine.invoke(address, method, args.toArray) match {
          case results: Array[_] =>
            lua.pushBoolean(true)
            results.foreach(result => lua.pushValue(result))
            1 + results.length
          case _ =>
            lua.pushBoolean(true)
            1
        }
      }
      catch {
        case e: Throwable =>
          if (Settings.get.logLuaCallbackErrors && !e.isInstanceOf[LimitReachedException]) {
            OpenComputers.log.log(Level.WARNING, "Exception in Lua callback.", e)
          }
          e match {
            case _: LimitReachedException =>
              0
            case e: IllegalArgumentException if e.getMessage != null =>
              lua.pushBoolean(false)
              lua.pushString(e.getMessage)
              2
            case e: Throwable if e.getMessage != null =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString(e.getMessage)
              if (Settings.get.logLuaCallbackErrors) {
                lua.pushString(e.getStackTraceString.replace("\r\n", "\n"))
                4
              }
              else 3
            case _: IndexOutOfBoundsException =>
              lua.pushBoolean(false)
              lua.pushString("index out of bounds")
              2
            case _: IllegalArgumentException =>
              lua.pushBoolean(false)
              lua.pushString("bad argument")
              2
            case _: NoSuchMethodException =>
              lua.pushBoolean(false)
              lua.pushString("no such method")
              2
            case _: FileNotFoundException =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("file not found")
              3
            case _: SecurityException =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("access denied")
              3
            case _: IOException =>
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("i/o error")
              3
            case e: Throwable =>
              OpenComputers.log.log(Level.WARNING, "Unexpected error in Lua callback.", e)
              lua.pushBoolean(true)
              lua.pushNil()
              lua.pushString("unknown error")
              3
          }
      }
    })
    lua.setField(-2, "invoke")

    lua.pushScalaFunction(lua => {
      val address = lua.checkString(1)
      val method = lua.checkString(2)
      try {
        val doc = machine.documentation(address, method)
        if (Strings.isNullOrEmpty(doc))
          lua.pushNil()
        else
          lua.pushString(doc)
        1
      } catch {
        case e: NoSuchMethodException =>
          lua.pushNil()
          lua.pushString("no such method")
          2
        case t: Throwable =>
          lua.pushNil()
          lua.pushString(if (t.getMessage != null) t.getMessage else t.toString)
          2
      }
    })
    lua.setField(-2, "doc")

    lua.setGlobal("component")

    initPerms()

    lua.load(classOf[Machine].getResourceAsStream(Settings.scriptPath + "kernel.lua"), "=kernel", "t")
    lua.newThread() // Left as the first value on the stack.

    true
  }

  override def close() {
    super.close()

    if (lua != null) {
      lua.setTotalMemory(Integer.MAX_VALUE)
      lua.close()
    }
    lua = null
    kernelMemory = 0
  }

  // ----------------------------------------------------------------------- //

  // Transition to storing the 'are we in or returning from a sync call' in here
  // so we don't need to check the state. Will need a period where saves are
  // loaded using the old *and* new method and saved using the new.
  @Deprecated
  private def state = machine.asInstanceOf[Machine].state

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    // Unlimit memory use while unpersisting.
    lua.setTotalMemory(Integer.MAX_VALUE)

    try {
      // Try unpersisting Lua, because that's what all of the rest depends
      // on. First, clear the stack, meaning the current kernel.
      lua.setTop(0)

      unpersist(nbt.getByteArray("kernel"))
      if (!lua.isThread(1)) {
        // This shouldn't really happen, but there's a chance it does if
        // the save was corrupt (maybe someone modified the Lua files).
        throw new IllegalArgumentException("Invalid kernel.")
      }
      if (state.contains(Machine.State.SynchronizedCall) || state.contains(Machine.State.SynchronizedReturn)) {
        unpersist(nbt.getByteArray("stack"))
        if (!(if (state.contains(Machine.State.SynchronizedCall)) lua.isFunction(2) else lua.isTable(2))) {
          // Same as with the above, should not really happen normally, but
          // could for the same reasons.
          throw new IllegalArgumentException("Invalid stack.")
        }
      }

      kernelMemory = (nbt.getInteger("kernelMemory") * ramScale).toInt
    } catch {
      case e: LuaRuntimeException =>
        OpenComputers.log.warning("Could not unpersist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        machine.stop()
    }

    // Limit memory again.
    recomputeMemory()
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    // Unlimit memory while persisting.
    lua.setTotalMemory(Integer.MAX_VALUE)

    try {
      // Try persisting Lua, because that's what all of the rest depends on.
      // Save the kernel state (which is always at stack index one).
      assert(lua.isThread(1))
      nbt.setByteArray("kernel", persist(1))
      // While in a driver call we have one object on the global stack: either
      // the function to call the driver with, or the result of the call.
      if (state.contains(Machine.State.SynchronizedCall) || state.contains(Machine.State.SynchronizedReturn)) {
        assert(if (state.contains(Machine.State.SynchronizedCall)) lua.isFunction(2) else lua.isTable(2))
        nbt.setByteArray("stack", persist(2))
      }

      nbt.setInteger("kernelMemory", math.ceil(kernelMemory / ramScale).toInt)
    } catch {
      case e: LuaRuntimeException =>
        OpenComputers.log.warning("Could not persist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        nbt.removeTag("state")
    }

    // Limit memory again.
    recomputeMemory()
  }

  private def initPerms() {
    // These tables must contain all java callbacks (i.e. C functions, since
    // they are wrapped on the native side using a C function, of course).
    // They are used when persisting/unpersisting the state so that the
    // persistence library knows which values it doesn't have to serialize
    // (since it cannot persist C functions).
    lua.newTable() /* ... perms */
    lua.newTable() /* ... uperms */

    val perms = lua.getTop - 1
    val uperms = lua.getTop

    def flattenAndStore() {
      /* ... k v */
      // We only care for tables and functions, any value types are safe.
      if (lua.isFunction(-1) || lua.isTable(-1)) {
        lua.pushValue(-2) /* ... k v k */
        lua.getTable(uperms) /* ... k v uperms[k] */
        assert(lua.isNil(-1), "duplicate permanent value named " + lua.toString(-3))
        lua.pop(1) /* ... k v */
        // If we have aliases its enough to store the value once.
        lua.pushValue(-1) /* ... k v v */
        lua.getTable(perms) /* ... k v perms[v] */
        val isNew = lua.isNil(-1)
        lua.pop(1) /* ... k v */
        if (isNew) {
          lua.pushValue(-1) /* ... k v v */
          lua.pushValue(-3) /* ... k v v k */
          lua.rawSet(perms) /* ... k v ; perms[v] = k */
          lua.pushValue(-2) /* ... k v k */
          lua.pushValue(-2) /* ... k v k v */
          lua.rawSet(uperms) /* ... k v ; uperms[k] = v */
          // Recurse into tables.
          if (lua.isTable(-1)) {
            // Enforce a deterministic order when determining the keys, to ensure
            // the keys are the same when unpersisting again.
            val key = lua.toString(-2)
            val childKeys = mutable.ArrayBuffer.empty[String]
            lua.pushNil() /* ... k v nil */
            while (lua.next(-2)) {
              /* ... k v ck cv */
              lua.pop(1) /* ... k v ck */
              childKeys += lua.toString(-1)
            }
            /* ... k v */
            childKeys.sortWith((a, b) => a.compareTo(b) < 0)
            for (childKey <- childKeys) {
              lua.pushString(key + "." + childKey) /* ... k v ck */
              lua.getField(-2, childKey) /* ... k v ck cv */
              flattenAndStore() /* ... k v */
            }
            /* ... k v */
          }
          /* ... k v */
        }
        /* ... k v */
      }
      lua.pop(2) /* ... */
    }

    // Mark everything that's globally reachable at this point as permanent.
    lua.pushString("_G") /* ... perms uperms k */
    lua.getGlobal("_G") /* ... perms uperms k v */

    flattenAndStore() /* ... perms uperms */
    lua.setField(LuaState.REGISTRYINDEX, "uperms") /* ... perms */
    lua.setField(LuaState.REGISTRYINDEX, "perms") /* ... */
  }

  private def persist(index: Int): Array[Byte] = {
    lua.getGlobal("eris") /* ... eris */
    lua.getField(-1, "persist") /* ... eris persist */
    if (lua.isFunction(-1)) {
      lua.getField(LuaState.REGISTRYINDEX, "perms") /* ... eris persist perms */
      lua.pushValue(index) // ... eris persist perms obj
      try {
        lua.call(2, 1) // ... eris str?
      } catch {
        case e: Throwable =>
          lua.pop(1)
          throw e
      }
      if (lua.isString(-1)) {
        // ... eris str
        val result = lua.toByteArray(-1)
        lua.pop(2) // ...
        return result
      } // ... eris :(
    } // ... eris :(
    lua.pop(2) // ...
    Array[Byte]()
  }

  private def unpersist(value: Array[Byte]): Boolean = {
    lua.getGlobal("eris") // ... eris
    lua.getField(-1, "unpersist") // ... eris unpersist
    if (lua.isFunction(-1)) {
      lua.getField(LuaState.REGISTRYINDEX, "uperms") /* ... eris persist uperms */
      lua.pushByteArray(value) // ... eris unpersist uperms str
      lua.call(2, 1) // ... eris obj
      lua.insert(-2) // ... obj eris
      lua.pop(1)
      return true
    } // ... :(
    lua.pop(1)
    false
  }
}