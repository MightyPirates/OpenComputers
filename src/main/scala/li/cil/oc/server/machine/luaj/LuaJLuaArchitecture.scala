package li.cil.oc.server.machine.luaj

import java.io.FileNotFoundException
import java.io.IOException

import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.item.Memory
import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.machine.ExecutionResult
import li.cil.oc.api.machine.LimitReachedException
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.ScalaClosure
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2._
import li.cil.repack.org.luaj.vm2.lib.jse.JsePlatform
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

import scala.collection.convert.ImplicitConversionsToScala._

@Architecture.Name("LuaJ")
class LuaJLuaArchitecture(val machine: api.machine.Machine) extends Architecture {
  private[machine] var lua: Globals = _

  private var thread: LuaThread = _

  private var synchronizedCall: LuaFunction = _

  private var synchronizedResult: LuaValue = _

  private var doneWithInitRun = false

  private[machine] var memory = 0

  private val apis = Array(
    new ComponentAPI(this),
    new ComputerAPI(this),
    new OSAPI(this),
    new SystemAPI(this),
    new UnicodeAPI(this),
    new UserdataAPI(this))

  private[machine] def invoke(f: () => Array[AnyRef]): Varargs = try {
    f() match {
      case results: Array[_] =>
        LuaValue.varargsOf(Array(LuaValue.TRUE) ++ results.map(toLuaValue))
      case _ =>
        LuaValue.TRUE
    }
  }
  catch {
    case e: Throwable =>
      if (Settings.get.logLuaCallbackErrors && !e.isInstanceOf[LimitReachedException]) {
        OpenComputers.log.warn("Exception in Lua callback.", e)
      }
      e match {
        case _: LimitReachedException =>
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
          OpenComputers.log.warn("Unexpected error in Lua callback.", e)
          LuaValue.varargsOf(LuaValue.TRUE, LuaValue.NIL, LuaValue.valueOf("unknown error"))
      }
  }

  private[machine] def documentation(f: () => String): Varargs = try {
    val doc = f()
    if (Strings.isNullOrEmpty(doc)) LuaValue.NIL
    else LuaValue.valueOf(doc)
  }
  catch {
    case e: NoSuchMethodException =>
      LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such method"))
    case t: Throwable =>
      LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(if (t.getMessage != null) t.getMessage else t.toString))
  }

  // ----------------------------------------------------------------------- //

  override def isInitialized = doneWithInitRun

  override def recomputeMemory(components: java.lang.Iterable[ItemStack]) = {
    memory = memoryInBytes(components)
    memory > 0
  }

  private def memoryInBytes(components: java.lang.Iterable[ItemStack]) = components.foldLeft(0.0)((acc, stack) => acc + (Option(api.Driver.driverFor(stack)) match {
    case Some(driver: Memory) => driver.amount(stack) * 1024
    case _ => 0
  })).toInt max 0 min Settings.get.maxTotalRam

  // ----------------------------------------------------------------------- //

  override def runSynchronized() {
    synchronizedResult = synchronizedCall.call()
    synchronizedCall = null
  }

  override def runThreaded(isSynchronizedReturn: Boolean) = {
    try {
      // Resume the Lua state and remember the number of results we get.
      val results = if (isSynchronizedReturn) {
        // If we were doing a synchronized call, continue where we left off.
        val result = thread.resume(synchronizedResult)
        synchronizedResult = null
        result
      }
      else {
        if (!doneWithInitRun) {
          // We're doing the initialization run.
          val result = thread.resume(LuaValue.NONE)
          // Mark as done *after* we ran, to avoid switching to synchronized
          // calls when we actually need direct ones in the init phase.
          doneWithInitRun = true
          // We expect to get nothing here, if we do we had an error.
          if (result.narg != 1) {
            result
          }
          else {
            // Fake zero sleep to avoid stopping if there are no signals.
            LuaValue.varargsOf(LuaValue.TRUE, LuaValue.valueOf(0))
          }
        }
        else machine.popSignal() match {
          case signal if signal != null =>
            thread.resume(LuaValue.varargsOf(Array(LuaValue.valueOf(signal.name)) ++ signal.args.map(ScalaClosure.toLuaValue)))
          case _ =>
            thread.resume(LuaValue.NONE)
        }
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
        // This is a little... messy because we run a pcall inside the kernel
        // to be able to catch errors before JNLua gets its claws on them. So
        // we can either have (boolean, string | error) if the main kernel
        // fails, or (boolean, boolean, string | error) if something inside
        // that pcall goes bad.
        def isInnerError = results.`type`(2) == LuaValue.TBOOLEAN && (results.isstring(3) || results.isnoneornil(3))
        def isOuterError = results.isstring(2) || results.isnoneornil(2)
        if (results.`type`(1) != LuaValue.TBOOLEAN || !isInnerError || !isOuterError) {
          OpenComputers.log.warn("Kernel returned unexpected results.")
        }
        // The pcall *should* never return normally... but check for it nonetheless.
        if ((isOuterError && results.toboolean(1)) || (isInnerError && results.toboolean(2))) {
          OpenComputers.log.warn("Kernel stopped unexpectedly.")
          new ExecutionResult.Shutdown(false)
        }
        else {
          val error =
            if (isInnerError)
              if (results.isuserdata(3)) results.touserdata(3).toString
              else results.tojstring(3)
            else if (results.isuserdata(2)) results.touserdata(2).toString
            else results.tojstring(2)
          if (error != null) new ExecutionResult.Error(error)
          else new ExecutionResult.Error("unknown error")
        }
      }
    }
    catch {
      case e: LuaError =>
        OpenComputers.log.warn("Kernel crashed. This is a bug!", e)
        new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it")
      case e: Throwable =>
        OpenComputers.log.warn("Unexpected error in kernel. This is a bug!", e)
        new ExecutionResult.Error("kernel panic: this is a bug, check your log file and report it")
    }
  }

  override def onSignal(): Unit = {}

  // ----------------------------------------------------------------------- //

  override def initialize() = {
    lua = JsePlatform.debugGlobals()
    lua.set("package", LuaValue.NIL)
    lua.set("require", LuaValue.NIL)
    lua.set("io", LuaValue.NIL)
    lua.set("os", LuaValue.NIL)
    lua.set("luajava", LuaValue.NIL)

    // Remove some other functions we don't need and are dangerous.
    lua.set("dofile", LuaValue.NIL)
    lua.set("loadfile", LuaValue.NIL)

    apis.foreach(_.initialize())

    recomputeMemory(machine.host.internalComponents)

    val kernel = lua.load(classOf[Machine].getResourceAsStream(Settings.scriptPath + "machine.lua"), "=machine", "t", lua)
    thread = new LuaThread(lua, kernel) // Left as the first value on the stack.

    true
  }

  override def onConnect() {
  }

  override def close() = {
    lua = null
    thread = null
    synchronizedCall = null
    synchronizedResult = null
    doneWithInitRun = false
  }

  // ----------------------------------------------------------------------- //

  override def loadData(nbt: CompoundNBT) {
    if (machine.isRunning) {
      machine.stop()
      machine.start()
    }
  }

  override def saveData(nbt: CompoundNBT) {}
}
