package li.cil.oc.server.computer

import java.util.concurrent._
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import scala.Array.canBuildFrom
import scala.collection.JavaConversions._

import com.naef.jnlua._

import li.cil.oc.Config
import li.cil.oc.common.computer.IInternalComputerContext
import net.minecraft.nbt._

class Computer(val owner: IComputerEnvironment) extends IInternalComputerContext with Runnable {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  /** The internal Lua state. Only set while the computer is running. */
  private var lua: LuaState = null

  /**
   * The base memory consumption of the kernel. Used to permit a fixed base
   * memory for user-space programs even if the amount of memory the kernel
   * uses changes over time (i.e. with future releases of the mod). This is set
   * when starting up the computer.
   */
  private var baseMemory = 0

  /**
   * The time when the computer was started. This is used for our custom
   * implementation of os.clock(), which returns the amount of the time the
   * computer has been running.
   */
  private var timeStarted = 0.0

  /**
   * The current execution state of the computer. This is used to track how to
   * resume the computers main thread, if at all, and whether to accept new
   * signals or not.
   */
  private var state = State.Stopped

  /**
   * The queue of signals the Lua state should process. Signals are queued from
   * the Java side and processed one by one in the Lua VM. They are the only
   * means to communicate actively with the computer (passively only drivers
   * can interact with the computer by providing API functions).
   */
  private val signals = new LinkedBlockingQueue[Signal](256)

  /**
   * This is used to keep track of the current executor of the Lua state, for
   * example to wait for the computer to finish running a task. This is used to
   * cancel scheduled execution when a new signal arrives and to wait for the
   * computer to shut down.
   */
  private var future: Future[_] = null

  /**
   * This lock is used by the thread executing the Lua state when it performs
   * a synchronized API call. In that case it acquires this lock and waits for
   * the server thread. The server thread will try to acquire the lock after
   * notifying the state thread, to make sure the call was complete before
   * resuming.
   */
  private val driverLock = new ReentrantLock()

  /**
   * The last time (system time) the update function was called by the server
   * thread. We use this to detect whether the game was paused, to also pause
   * the executor thread for our Lua state.
   */
  private var lastUpdate = 0L

  /**
   * The object our executor thread waits on if the last update has been a
   * while, and the update function calls notify on each time it is run.
   */
  private val updateMonitor = new Object()

  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** Starts asynchronous execution of this computer if it isn't running. */
  def start(): Boolean = state match {
    case State.Stopped => {
      if (init()) {
        state = State.Running
        future = Executor.pool.submit(this)
        true
      }
      else false
    }
    case _ => false
  }

  /** Stops a computer asynchronously. */
  def stop(): Unit = if (state != State.Stopped) {
    signals.clear()
    signal(0, "terminate")
  }

  /** Stops a computer synchronously. */
  def stopAndWait(): Unit = {
    stop()
    // Get a local copy to avoid having to synchronize it between the null
    // check and the actual wait.
    val future = this.future
    if (future != null) future.get()
  }

  // ----------------------------------------------------------------------- //
  // IComputerContext
  // ----------------------------------------------------------------------- //

  def luaState = lua

  def update() {
    updateMonitor.synchronized {
      if (state == State.Stopped) return

      // Check if executor is waiting for a lock to interact with a driver.
      future.synchronized {
        if (state == State.Synchronizing) {
          // Thread is waiting to perform synchronized API call, notify it.
          future.notify()
          // Wait until the API call completed, which is when the driver lock
          // becomes available again (we lock it in the executor thread before
          // waiting to be notified). We need an extra lock for that because the
          // driver will release the lock on 'future' to do so (see lock()).
          driverLock.lock()
          driverLock.unlock()
        }
      }

      // Update last time run to let our executor thread know it doesn't have to
      // pause, and wake it up if it did pause (because the game was paused).
      lastUpdate = System.currentTimeMillis()
      updateMonitor.notify()
    }
  }

  def signal(pid: Int, name: String, args: Any*) = {
    args.foreach {
      case _: Byte | _: Short | _: Int | _: Long | _: Float | _: Double | _: String => Unit
      case _ => throw new IllegalArgumentException()
    }
    if (state != State.Stopped) {
      signals.offer(new Signal(pid, name, Array(args)))
      // TODO cancel delayed future and schedule for immediate execution
      //      if (this.synchronized(!signals.isEmpty() && state == State.Stopped)) {
      //        state = State.Running
      //        Executor.pool.execute(this)
      //      }
    }
  }

  def lock() {
    driverLock.lock()
    future.synchronized {
      state = State.Synchronizing
      future.wait()
    }
  }

  def unlock() {
    driverLock.unlock()
  }

  def readFromNBT(nbt: NBTTagCompound): Unit = {
    // If we're running we wait for the executor to yield, to get the Lua state
    // into a valid, suspended state before trying to unpersist into it.
    this.synchronized {
      state = State(nbt.getInteger("state"))
      if (state != State.Stopped && (lua != null || init())) {
        baseMemory = nbt.getInteger("baseMemory")
        timeStarted = nbt.getDouble("timeStarted")

        val memory = lua.getTotalMemory()
        lua.setTotalMemory(Integer.MAX_VALUE)
        val kernel = nbt.getString("kernel")
        lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.unpersist)
        lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.unpersistTable)
        lua.pushString(kernel)
        lua.call(2, 1)
        lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.kernel)
        lua.setTotalMemory(memory)

        signals.clear()
        val signalsTag = nbt.getTagList("signals")
        signals.addAll((0 until signalsTag.tagCount()).
          map(signalsTag.tagAt(_).asInstanceOf[NBTTagCompound]).
          map(signal => {
            val argsTag = signal.getCompoundTag("args")
            val argsLength = argsTag.getInteger("length")
            new Signal(signal.getInteger("pid"), signal.getString("name"),
              (0 until argsLength).map("arg" + _).map(argsTag.getTag(_)).map {
                case tag: NBTTagByte => tag.data
                case tag: NBTTagShort => tag.data
                case tag: NBTTagInt => tag.data
                case tag: NBTTagLong => tag.data
                case tag: NBTTagFloat => tag.data
                case tag: NBTTagDouble => tag.data
                case tag: NBTTagString => tag.data
              }.toArray)
          }))

        lua.gc(LuaState.GcAction.COLLECT, 0)

        // Start running our worker thread if we don't already have one.
        if (future == null) future = Executor.pool.submit(this)
      }
    }
  }

  def writeToNBT(nbt: NBTTagCompound): Unit = {
    // If we're running we wait for the executor to yield, to get the Lua state
    // into a valid, suspended state before trying to persist it.
    this.synchronized {
      nbt.setInteger("state", state.id)
      if (state == State.Stopped) return

      nbt.setInteger("baseMemory", baseMemory)
      nbt.setDouble("timeStarted", timeStarted)

      // Call pluto.persist(persistTable, _G) and store the string result.
      val memory = lua.getTotalMemory()
      lua.setTotalMemory(Integer.MAX_VALUE)
      lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.persist)
      lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.persistTable)
      lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.kernel)
      lua.call(2, 1)
      val kernel = lua.toString(-1)
      lua.pop(1)
      nbt.setString("kernel", kernel)
      lua.setTotalMemory(memory)

      val list = new NBTTagList()
      for (s <- signals.iterator()) {
        val signal = new NBTTagCompound()
        signal.setInteger("pid", s.pid)
        signal.setString("name", s.name)
        // TODO Test with NBTTagList, but supposedly it only allows entries
        //      with the same type, so I went with this for now...
        val args = new NBTTagCompound()
        args.setInteger("length", s.args.length)
        s.args.zipWithIndex.foreach {
          case (arg: Byte, i) => args.setByte("arg" + i, arg)
          case (arg: Short, i) => args.setShort("arg" + i, arg)
          case (arg: Int, i) => args.setInteger("arg" + i, arg)
          case (arg: Long, i) => args.setLong("arg" + i, arg)
          case (arg: Float, i) => args.setFloat("arg" + i, arg)
          case (arg: Double, i) => args.setDouble("arg" + i, arg)
          case (arg: String, i) => args.setString("arg" + i, arg)
        }
        signal.setCompoundTag("args", args)
        list.appendTag(signal)
      }
      nbt.setTag("signals", list)

      lua.gc(LuaState.GcAction.COLLECT, 0)
    }
  }

  def init(): Boolean = {
    // Creates a new state with all base libraries as well as the Pluto
    // library loaded into it. This means the state has much more power than
    // it rightfully should have, so we sandbox it a bit in the following.
    lua = LuaStateFactory.createState()

    try {
      // Before doing the actual sandboxing we save the Pluto library into the
      // registry, since it'll be removed from the globals table.
      lua.getGlobal("eris")
      lua.getField(-1, "persist")
      lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.persist)
      lua.getField(-1, "unpersist")
      lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.unpersist)
      lua.pop(1)

      // Push a couple of functions that override original Lua API functions or
      // that add new functionality to it.
      lua.getGlobal("os")

      // Return ingame time for os.time().
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          // Minecraft starts days at 6 o'clock, so we add six hours.
          lua.pushNumber((owner.world.getTotalWorldTime() + 6000.0) / 1000.0)
          return 1
        }
      })
      lua.setField(-2, "time")

      // Custom os.clock() implementation returning the time the computer has
      // been running, instead of the native library...
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          lua.pushNumber(owner.world.getTotalWorldTime() - timeStarted)
          return 1
        }
      })
      lua.setField(-2, "clock")

      // TODO Other overrides?

      // Pop the os table.
      lua.pop(1)

      // Run the sandboxing script. This script is presumed to be under our
      // control. We do the sandboxing in Lua because it'd be a pain to write
      // using only stack operations...
      lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/sandbox.lua"), "sandbox", "t")
      lua.call(0, 0)

      // Install all driver callbacks into the registry. This is done once in
      // the beginning so that we can take the memory the callbacks use into
      // account when computing the kernel's memory use, as well as for building
      // a table of permanent values used when persisting/unpersisting the state.
      Drivers.injectInto(this)

      // Run the script that builds the tables with permanent values. These
      // tables must contain all java callbacks (i.e. C functions, since they
      // are wrapped on the native side using a C function, of course). They
      // are used when persisting/unpersisting the state so that Pluto knows
      // which values it doesn't have to serialize (since it cannot persist C
      // functions). We store the two tables in the registry.
      // TODO These tables may change after loading a game, for example due to
      // a new mod being installed or an old one being removed. In that case,
      // previously existing values will "suddenly" become nil. We may want to
      // consider detecting such changes and rebooting computers in that case.
      lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/persistence.lua"), "persistence", "t")
      lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.driverApis)
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          println(lua.toString(1))
          return 0
        }
      })
      lua.call(2, 2)
      lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.unpersistTable)
      lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.persistTable)

      // Load the basic kernel which takes care of handling signals by managing
      // the list of active processes. Whatever functionality we can we implement
      // in Lua, so we also implement most of the kernel's functionality in Lua.
      // Why? Because like this it's automatically persisted for us without
      // having to write more additional NBT stuff.
      lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/kernel.lua"), "kernel", "t")
      lua.newThread()
      lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.kernel)

      // Run the garbage collector to get rid of stuff left behind after the
      // initialization phase to get a good estimate of the base memory usage
      // the kernel has. We remember that size to grant user-space programs a
      // fixed base amount of memory.
      lua.gc(LuaState.GcAction.COLLECT, 0)
      baseMemory = lua.getTotalMemory() - lua.getFreeMemory()
      lua.setTotalMemory(baseMemory + 128 * 1024)

      // Remember when we started the computer.
      timeStarted = System.currentTimeMillis()

      println("Kernel uses " + baseMemory + " bytes of memory.")

      return true
    }
    catch {
      case ex: Throwable => {
        ex.printStackTrace()
        close()
      }
    }
    return false
  }

  def close() {
    lua.setTotalMemory(Integer.MAX_VALUE);
    lua.close()
    lua = null
    baseMemory = 0
    timeStarted = 0
    state = State.Stopped
    future = null
    signals.clear()
  }

  def run() {
    try {
      println("start running computer")

      // See if the game appears to be paused, in which case we also pause.
      if (System.currentTimeMillis() - lastUpdate > 500)
        updateMonitor.synchronized {
          updateMonitor.wait()
        }

      println("running computer")

      // This is synchronized so that we don't run a Lua state while saving or
      // loading the computer to or from an NBTTagCompound.
      this.synchronized {
        // Push the kernel coroutine onto the stack so that we can resume it.
        lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.kernel)
        // Get a copy to check the coroutine's status after it ran.
        lua.pushValue(-1)

        try {
          // Resume the Lua state and remember the number of results we get.
          val results = state match {
            // Current coroutine was forced to yield. Resume without injecting any
            // signals. Any passed arguments would simply be ignored.
            case State.Yielding => {
              println("resuming forced yield")
              lua.resume(-1, 0)
            }

            // We're running normally, i.e. all coroutines yielded voluntarily and
            // this yield comes directly out of the main kernel coroutine.
            case _ => {
              // Try to get a signal to run the state with.
              signals.poll() match {
                // No signal, just run any non-sleeping processes.
                case null => {
                  println("resuming without signal")
                  lua.resume(-1, 0)
                }

                // Got a signal, inject it and call any handlers (if any).
                case signal => {
                  println("injecting signal")
                  lua.pushInteger(signal.pid)
                  lua.pushString(signal.name)
                  signal.args.foreach {
                    case arg: Byte => lua.pushInteger(arg)
                    case arg: Short => lua.pushInteger(arg)
                    case arg: Integer => lua.pushInteger(arg)
                    case arg: Long => lua.pushNumber(arg)
                    case arg: Float => lua.pushNumber(arg)
                    case arg: Double => lua.pushNumber(arg)
                    case arg: String => lua.pushString(arg)
                  }
                  lua.resume(-1, 2 + signal.args.length)
                }
              }
            }
          }

          println("lua yielded")

          // Only queue for next execution step if the kernel is still alive.
          if (lua.status(-(results + 1)) != 0) {
            // See what we have. The convention is that if the first result is a
            // string with the value "timeout" the currently running coroutines was
            // forced to yield by the execution limit (i.e. the yield comes from the
            // debug hook we installed as seen in the sandbox.lua script). Otherwise
            // it's a normal yield, and we get the time to wait before we should try
            // to execute the state again in seconds.
            if (lua.isString(-results) && "timeout".equals(lua.toString(-results))) {
              // Forced yield due to long execution time. Remember this for the next
              // time we run, so we don't try to insert a signal which would get
              // ignored.
              state = State.Yielding
              future = Executor.pool.submit(this)
            }
            else {
              // Lua state yielded normally, see how long we should wait before
              // resuming the state again.
              val sleep = (lua.toNumber(-1) * 1000).toLong
              state = State.Running
              future = Executor.pool.schedule(this, sleep, TimeUnit.MILLISECONDS)
            }
          }
          lua.pop(results)
        }
        catch {
          // The kernel should never throw. If it does, the computer crashed
          // hard, so we just close the state.
          // TODO Print something to an in-game screen, a la kernel panic.
          case ex: LuaRuntimeException => ex.printLuaStackTrace()
          case ex: Throwable => ex.printStackTrace()
        }
        println("free memory: " + lua.getFreeMemory())

        // If the kernel is no longer running the computer has stopped.
        lua.status(-1) match {
          case LuaState.YIELD => lua.pop(1)
          case _ => updateMonitor.synchronized(close())
        }
      }

      println("end running computer")
    }
    catch {
      case t: Throwable => t.printStackTrace()
    }
  }

  /** Signals are messages sent to the Lua state's processes from Java. */
  private class Signal(val pid: Int, val name: String, val args: Array[Any]) {
  }

  /** Possible states of the computer, and in particular its executor. */
  private object State extends Enumeration {
    /** Self explanatory: the computer is not running right now. */
    val Stopped = Value("Stopped")

    /** The computer is up and running, executing Lua code. */
    val Running = Value("Running")

    /** The computer is yielding because of its execution limit. */
    val Yielding = Value("Yielding")

    /** The computer executor is waiting for the server thread. */
    val Synchronizing = Value("Synchronizing")
  }

  /** Singleton for requesting executors that run our Lua states. */
  private object Executor {
    val pool = Executors.newScheduledThreadPool(Config.threads, new ThreadFactory() {
      private val threadNumber = new AtomicInteger(1)

      private val group = System.getSecurityManager() match {
        case null => Thread.currentThread().getThreadGroup()
        case s => s.getThreadGroup()
      }

      def newThread(r: Runnable): Thread = {
        val name = "OpenComputers-" + threadNumber.getAndIncrement()
        val thread = new Thread(group, r, name, 0)
        if (thread.isDaemon())
          thread.setDaemon(false)
        if (thread.getPriority() != Thread.MIN_PRIORITY)
          thread.setPriority(Thread.MIN_PRIORITY)
        return thread
      }
    })
  }
}

/** Names of entries in the registries of the Lua states of computers. */
private[computer] object ComputerRegistry {
  val kernel = "oc_kernel"
  val driverApis = "oc_apis"
  val persist = "oc_persist"
  val unpersist = "oc_unpersist"
  val unpersistTable = "oc_unpersistTable"
  val persistTable = "oc_persistTable"
}
