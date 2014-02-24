package li.cil.oc.server.component.machine

import Machine.State
import java.io.{IOException, FileNotFoundException}
import java.util.logging.Level
import li.cil.oc.util.ScalaClosure._
import li.cil.oc.util.{ScalaClosure, GameTimeFormatter}
import li.cil.oc.{OpenComputers, server, Settings}
import net.minecraft.nbt.NBTTagCompound
import org.luaj.vm3._
import org.luaj.vm3.lib.jse.JsePlatform
import scala.Some
import scala.collection.convert.WrapAsScala._

class LuaJLuaArchitecture(machine: Machine) extends LuaArchitecture(machine) {
  private var lua: Globals = _

  private var thread: LuaThread = _

  private var synchronizedCall: LuaFunction = _

  private var synchronizedResult: LuaValue = _

  private var doneWithInitRun = false

  private var memory = 0

  // ----------------------------------------------------------------------- //

  private def node = machine.node

  private def components = machine.components

  // ----------------------------------------------------------------------- //

  override def isInitialized = doneWithInitRun

  override def recomputeMemory() = memory = machine.owner.installedMemory

  // ----------------------------------------------------------------------- //

  override def runSynchronized() {
    synchronizedResult = synchronizedCall.call()
    synchronizedCall = null
  }

  override def runThreaded(enterState: State.Value) = {
    try {
      // Resume the Lua state and remember the number of results we get.
      val results = enterState match {
        case Machine.State.SynchronizedReturn =>
          // If we were doing a synchronized call, continue where we left off.
          val result = thread.resume(synchronizedResult)
          synchronizedResult = null
          result
        case Machine.State.Yielded =>
          if (!doneWithInitRun) {
            // We're doing the initialization run.
            val result = thread.resume(LuaValue.NONE)
            // Mark as done *after* we ran, to avoid switching to synchronized
            // calls when we actually need direct ones in the init phase.
            doneWithInitRun = true
            // We expect to get nothing here, if we do we had an error.
            if (result.narg == 1) {
              // Fake zero sleep to avoid stopping if there are no signals.
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.valueOf(0))
            }
            else {
              LuaValue.NONE
            }
          }
          else machine.popSignal() match {
            case Some(signal) =>
              thread.resume(LuaValue.varargsOf(Array(LuaValue.valueOf(signal.name)) ++ signal.args.map(ScalaClosure.toLuaValue)))
            case _ =>
              thread.resume(LuaValue.NONE)
          }
        case s => throw new AssertionError("Running computer from invalid state " + s.toString)
      }

      // Check if the kernel is still alive.
      if (thread.state.status == LuaThread.STATUS_SUSPENDED) {
        // If we get one function it must be a wrapper for a synchronized
        // call. The protocol is that a closure is pushed that is then called
        // from the main server thread, and returns a table, which is in turn
        // passed to the originating coroutine.yield().
        if (results.narg == 2 && results.isfunction(2)) {
          synchronizedCall = results.checkfunction(2)
          new ExecutionResult.SynchronizedCall()
        }
        // Check if we are shutting down, and if so if we're rebooting. This
        // is signalled by boolean values, where `false` means shut down,
        // `true` means reboot (i.e shutdown then start again).
        else if (results.narg == 2 && results.`type`(2) == LuaValue.TBOOLEAN) {
          new ExecutionResult.Shutdown(results.toboolean(2))
        }
        else {
          // If we have a single number, that's how long we may wait before
          // resuming the state again. Note that the sleep may be interrupted
          // early if a signal arrives in the meantime. If we have something
          // else we just process the next signal or wait for one.
          val ticks = if (results.narg == 2 && results.isnumber(2)) (results.todouble(2) * 20).toInt else Int.MaxValue
          new ExecutionResult.Sleep(ticks)
        }
      }
      // The kernel thread returned. If it threw we'd be in the catch below.
      else {
        // We're expecting the result of a pcall, if anything, so boolean + (result | string).
        if (results.`type`(2) != LuaValue.TBOOLEAN || !(results.isstring(3) || results.isnil(3))) {
          OpenComputers.log.warning("Kernel returned unexpected results.")
        }
        // The pcall *should* never return normally... but check for it nonetheless.
        if (results.toboolean(1)) {
          OpenComputers.log.warning("Kernel stopped unexpectedly.")
          new ExecutionResult.Shutdown(false)
        }
        else {
          val error = results.tojstring(3)
          if (error != null) new ExecutionResult.Error(error)
          else new ExecutionResult.Error("unknown error")
        }
      }
    }
    catch {
      case e: LuaError =>
        OpenComputers.log.log(Level.WARNING, "Kernel crashed. This is a bug!" + e)
        new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it")
      case e: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Unexpected error in kernel. This is a bug!", e)
        new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it")
    }
  }

  // ----------------------------------------------------------------------- //

  override def init() = {
    super.init()

    lua = JsePlatform.debugGlobals()
    lua.set("package", LuaValue.NIL)
    lua.set("io", LuaValue.NIL)
    lua.set("luajava", LuaValue.NIL)

    // Prepare table for os stuff.
    val os = LuaValue.tableOf()
    lua.set("os", os)

    // Remove some other functions we don't need and are dangerous.
    lua.set("dofile", LuaValue.NIL)
    lua.set("loadfile", LuaValue.NIL)

    // Provide some better Unicode support.
    val unicode = LuaValue.tableOf()

    unicode.set("lower", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).toLowerCase))

    unicode.set("upper", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).toUpperCase))

    unicode.set("char", (args: Varargs) => LuaValue.valueOf(String.valueOf((1 to args.narg).map(args.checkint).map(_.toChar).toArray)))

    unicode.set("len", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).length))

    unicode.set("reverse", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).reverse))

    unicode.set("sub", (args: Varargs) => {
      val string = args.checkjstring(1)
      val start = math.max(0, args.checkint(2) match {
        case i if i < 0 => string.length + i
        case i => i - 1
      })
      val end =
        if (args.narg > 2) math.min(string.length, args.checkint(3) match {
          case i if i < 0 => string.length + i + 1
          case i => i
        })
        else string.length
      if (end <= start) LuaValue.valueOf("")
      else LuaValue.valueOf(string.substring(start, end))
    })

    lua.set("unicode", unicode)

    os.set("clock", (_: Varargs) => LuaValue.valueOf((machine.cpuTime + (System.nanoTime() - machine.cpuStart)) * 10e-10))

    // Date formatting function.
    os.set("date", (args: Varargs) => {
      val format =
        if (args.narg > 0 && args.isstring(1)) args.tojstring(1)
        else "%d/%m/%y %H:%M:%S"
      val time =
        if (args.narg > 1 && args.isnumber(2)) args.todouble(2) * 1000 / 60 / 60
        else machine.worldTime + 6000

      val dt = GameTimeFormatter.parse(time)
      def fmt(format: String) = {
        if (format == "*t") {
          val table = LuaValue.tableOf(0, 8)
          table.set("year", LuaValue.valueOf(dt.year))
          table.set("month", LuaValue.valueOf(dt.month))
          table.set("day", LuaValue.valueOf(dt.day))
          table.set("hour", LuaValue.valueOf(dt.hour))
          table.set("min", LuaValue.valueOf(dt.minute))
          table.set("sec", LuaValue.valueOf(dt.second))
          table.set("wday", LuaValue.valueOf(dt.weekDay))
          table.set("yday", LuaValue.valueOf(dt.yearDay))
          table
        }
        else {
          LuaValue.valueOf(GameTimeFormatter.format(format, dt))
        }
      }

      // Just ignore the allowed leading '!', Minecraft has no time zones...
      if (format.startsWith("!"))
        fmt(format.substring(1))
      else
        fmt(format)
    })

    // Return ingame time for os.time().
    os.set("time", (_: Varargs) => {
      // Game time is in ticks, so that each day has 24000 ticks, meaning
      // one hour is game time divided by one thousand. Also, Minecraft
      // starts days at 6 o'clock, so we add those six hours. Thus:
      // timestamp = (time + 6000) * 60[kh] * 60[km] / 1000[s]
      LuaValue.valueOf((machine.worldTime + 6000) * 60 * 60 / 1000)
    })

    // Computer API, stuff that kinda belongs to os, but we don't want to
    // clutter it.
    val computer = LuaValue.tableOf()

    // Allow getting the real world time for timeouts.
    computer.set("realTime", (_: Varargs) => LuaValue.valueOf(System.currentTimeMillis() / 1000.0))

    // The time the computer has been running, as opposed to the CPU time.
    // World time is in ticks, and each second has 20 ticks. Since we
    // want uptime() to return real seconds, though, we'll divide it
    // accordingly.
    computer.set("uptime", (_: Varargs) => LuaValue.valueOf((machine.worldTime - machine.timeStarted) / 20.0))

    // Allow the computer to figure out its own id in the component network.
    computer.set("address", (_: Varargs) => Option(node.address) match {
      case Some(address) => LuaValue.valueOf(address)
      case _ => LuaValue.NIL
    })

    // Are we a robot? (No this is not a CAPTCHA.)
    computer.set("isRobot", (_: Varargs) => LuaValue.valueOf(machine.isRobot))

    computer.set("freeMemory", (_: Varargs) => LuaValue.valueOf(memory / 2))

    computer.set("totalMemory", (_: Varargs) => LuaValue.valueOf(memory))

    computer.set("pushSignal", (args: Varargs) => LuaValue.valueOf(machine.signal(args.checkjstring(1), toSimpleJavaObjects(args, 2): _*)))

    // And its ROM address.
    computer.set("romAddress", (_: Varargs) => rom.fold(LuaValue.NIL)(fs => Option(fs.node.address) match {
      case Some(address) => LuaValue.valueOf(address)
      case _ => LuaValue.NIL
    }))

    // And it's /tmp address...
    computer.set("tmpAddress", (_: Varargs) => machine.tmp.fold(LuaValue.NIL)(fs => Option(fs.node.address) match {
      case Some(address) => LuaValue.valueOf(address)
      case _ => LuaValue.NIL
    }))

    // User management.
    computer.set("users", (_: Varargs) => LuaValue.varargsOf(machine.users.map(LuaValue.valueOf)))

    computer.set("addUser", (args: Varargs) => {
      machine.addUser(args.checkjstring(1))
      LuaValue.TRUE
    })

    computer.set("removeUser", (args: Varargs) => LuaValue.valueOf(machine.removeUser(args.checkjstring(1))))

    computer.set("energy", (_: Varargs) => LuaValue.valueOf(node.globalBuffer))

    computer.set("maxEnergy", (_: Varargs) => LuaValue.valueOf(node.globalBufferSize))

    // Set the computer table.
    lua.set("computer", computer)

    // Whether bytecode may be loaded directly.
    lua.set("allowBytecode", (_: Varargs) => LuaValue.valueOf(Settings.get.allowBytecode))

    // How long programs may run without yielding before we stop them.
    lua.set("timeout", LuaValue.valueOf(Settings.get.timeout))

    // Component interaction stuff.
    val component = LuaValue.tableOf()

    component.set("list", (args: Varargs) => components.synchronized {
      val filter = if (args.isstring(1)) Option(args.tojstring(1)) else None
      val table = LuaValue.tableOf(0, components.size)
      for ((address, name) <- components) {
        if (filter.isEmpty || name.contains(filter.get)) {
          table.set(address, name)
        }
      }
      table
    })

    component.set("type", (args: Varargs) => components.synchronized {
      components.get(args.checkjstring(1)) match {
        case Some(name: String) =>
          LuaValue.valueOf(name)
        case _ =>
          LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
      }
    })

    component.set("methods", (args: Varargs) => {
      Option(node.network.node(args.checkjstring(1))) match {
        case Some(component: server.network.Component) if component.canBeSeenFrom(node) || component == node =>
          val table = LuaValue.tableOf()
          for (method <- component.methods()) {
            table.set(method, LuaValue.valueOf(component.isDirect(method)))
          }
          table
        case _ =>
          LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
      }
    })

    component.set("invoke", (args: Varargs) => {
      val address = args.checkjstring(1)
      val method = args.checkjstring(2)
      val params = toSimpleJavaObjects(args, 3)
      try {
        machine.invoke(address, method, params) match {
          case results: Array[_] =>
            LuaValue.varargsOf(Array(LuaValue.TRUE) ++ results.map(toLuaValue))
          case _ =>
            LuaValue.TRUE
        }
      }
      catch {
        case e: Throwable =>
          if (Settings.get.logLuaCallbackErrors && !e.isInstanceOf[Machine.LimitReachedException]) {
            OpenComputers.log.log(Level.WARNING, "Exception in Lua callback.", e)
          }
          e match {
            case _: Machine.LimitReachedException =>
              LuaValue.NONE
            case e: IllegalArgumentException if e.getMessage != null =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(e.getMessage))
            case e: Throwable if e.getMessage != null =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf(e.getMessage))
            case _: IndexOutOfBoundsException =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("index out of bounds"))
            case _: IllegalArgumentException =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("bad argument"))
            case _: NoSuchMethodException =>
              LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf("no such method"))
            case _: FileNotFoundException =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("file not found"))
            case _: SecurityException =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("access denied"))
            case _: IOException =>
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("i/o error"))
            case e: Throwable =>
              OpenComputers.log.log(Level.WARNING, "Unexpected error in Lua callback.", e)
              LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("unknown error"))
          }
      }
    })

    lua.set("component", component)

    recomputeMemory()

    val kernel = lua.load(classOf[Machine].getResourceAsStream(Settings.scriptPath + "kernel.lua"), "=kernel", "t", lua)
    thread = new LuaThread(lua, kernel) // Left as the first value on the stack.

    true
  }

  override def close() = {
    super.close()

    lua = null
    thread = null
    synchronizedCall = null
    synchronizedResult = null
    doneWithInitRun = false
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    if (machine.isRunning) {
      machine.stop()
      machine.start()
    }
  }
}
