package li.cil.oc.server.computer

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

import scala.Array.canBuildFrom
import scala.collection.JavaConversions._
import scala.util.Random

import com.naef.jnlua._

import li.cil.oc.Config
import li.cil.oc.common.computer.IInternalComputerContext
import net.minecraft.nbt._

class Computer(val owner: IComputerEnvironment) extends IInternalComputerContext with Runnable {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  /**
   * The current execution state of the computer. This is used to track how to
   * resume the computers main thread, if at all, and whether to accept new
   * signals or not.
   */
  private var state = State.Stopped

  /** The internal Lua state. Only set while the computer is running. */
  private var lua: LuaState = null

  /**
   * The base memory consumption of the kernel. Used to permit a fixed base
   * memory for userland even if the amount of memory the kernel uses changes
   * over time (i.e. with future releases of the mod). This is set when
   * starting up the computer.
   */
  private var kernelMemory = 0

  /**
   * The queue of signals the Lua state should process. Signals are queued from
   * the Java side and processed one by one in the Lua VM. They are the only
   * means to communicate actively with the computer (passively only drivers
   * can interact with the computer by providing API functions).
   */
  private val signals = new LinkedBlockingQueue[Signal](256)

  // ----------------------------------------------------------------------- //

  /**
   * The time (world time) when the computer was started. This is used for our
   * custom implementation of os.clock(), which returns the amount of the time
   * the computer has been running.
   */
  private var timeStarted = 0L

  /**
   * The last time (system time) the update function was called by the server
   * thread. We use this to detect whether the game was paused, to also pause
   * the executor thread for our Lua state.
   */
  private var lastUpdate = 0L

  /**
   * The current world time. This is used for our custom implementation of
   * os.time(). This is updated by the server thread and read by the computer
   * thread, to avoid computer threads directly accessing the world state.
   */
  private var worldTime = 0L

  // ----------------------------------------------------------------------- //

  /**
   * This is used to keep track of the current executor of the Lua state, for
   * example to wait for the computer to finish running a task.
   */
  private var future: Future[_] = null

  /**
   * The object our executor thread waits on if the last update has been a
   * while, and the update function calls notify on each time it is run.
   */
  private val pauseMonitor = new Object()

  /** This is used to synchronize access to the state field. */
  private val stateMonitor = new Object()

  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** Starts asynchronous execution of this computer if it isn't running. */
  def start(): Boolean = stateMonitor.synchronized(
    state == State.Stopped && init() && {
      state = State.Suspended
      // Inject a dummy signal so that real one don't get swallowed. This way
      // we can just ignore the parameters the first time the kernel is run.
      signal("dummy")
      future = Executor.pool.submit(this)
      true
    })

  /** Stops a computer, possibly asynchronously. */
  def stop(): Unit = stateMonitor.synchronized {
    if (state != State.Stopped) {
      if (state != State.Running) {
        // If the computer is not currently running we can simply close it,
        // and cancel any pending future - which may already be running and
        // waiting for the stateMonitor, so we do a hard abort.
        if (future != null) {
          future.cancel(true)
        }
        close()
      }
      else {
        // Otherwise we enter an intermediate state to ensure the executor
        // truly stopped, before switching back to stopped to allow starting
        // the computer again. The executor will check for this state and
        // call close.
        state = State.Stopping
        // Make sure the thread isn't waiting for an update.
        pauseMonitor.synchronized(pauseMonitor.notify())
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // IComputerContext
  // ----------------------------------------------------------------------- //

  def luaState = lua

  def update() {
    stateMonitor.synchronized(state match {
      case State.Stopped | State.Stopping => return
      case State.DriverCall => {
        assert(lua.getTop() == 2)
        assert(lua.`type`(1) == LuaType.THREAD)
        assert(lua.`type`(2) == LuaType.FUNCTION)
        lua.resume(1, 1)
        assert(lua.getTop() == 2)
        assert(lua.`type`(2) == LuaType.TABLE)
        state = State.DriverReturn
        future = Executor.pool.submit(this)
      }
      case _ => /* nothing special to do */
    })

    // Remember when we started the computer for os.clock(). We do this in the
    // update because only then can we be sure the world is available.
    if (timeStarted == 0)
      timeStarted = owner.world.getWorldInfo().getWorldTotalTime()

    // Update world time for computer threads.
    worldTime = owner.world.getWorldInfo().getWorldTotalTime()

    if (worldTime % 40 == 0) {
      signal("test", "ha!")
    }

    // Update last time run to let our executor thread know it doesn't have to
    // pause, and wake it up if it did pause (because the game was paused).
    lastUpdate = System.currentTimeMillis

    // Tell the executor thread it may continue if it's waiting.
    pauseMonitor.synchronized(pauseMonitor.notify())
  }

  def signal(name: String, args: Any*) = {
    args.foreach {
      case _: Byte | _: Short | _: Int | _: Long | _: Float | _: Double | _: String => Unit
      case _ => throw new IllegalArgumentException()
    }
    stateMonitor.synchronized(state match {
      // We don't push new signals when stopped or shutting down.
      case State.Stopped | State.Stopping =>
      // Currently sleeping. Cancel that and start immediately.
      case State.Sleeping =>
        future.cancel(true)
        state = State.Suspended
        signals.offer(new Signal(name, args.toArray))
        future = Executor.pool.submit(this)
      // Running or in driver call or only a short yield, just push the signal.
      case _ =>
        signals.offer(new Signal(name, args.toArray))
    })
  }

  def readFromNBT(nbt: NBTTagCompound): Unit = this.synchronized {
    // Clear out what we currently have, if anything.
    stateMonitor.synchronized {
      assert(state != State.Running) // Lock on 'this' should guarantee this.
      stop()
    }

    state = State(nbt.getInteger("state"))

    if (state != State.Stopped && init()) {
      // Unlimit memory use while unpersisting.
      val memory = lua.getTotalMemory()
      lua.setTotalMemory(Integer.MAX_VALUE)
      try {
        // Try unpersisting Lua, because that's what all of the rest depends on.
        // Clear the stack (meaning the current kernel).
        lua.setTop(0)

        if (!unpersist(nbt.getByteArray("kernel")) || !lua.isThread(1)) {
          // This shouldn't really happen, but there's a chance it does if
          // the save was corrupt (maybe someone modified the Lua files).
          throw new IllegalStateException("Could not restore kernel.")
        }
        if (state == State.DriverCall || state == State.DriverReturn) {
          if (!unpersist(nbt.getByteArray("stack")) ||
            (state == State.DriverCall && !lua.isFunction(2)) ||
            (state == State.DriverReturn && !lua.isTable(2))) {
            // Same as with the above, should not really happen normally, but
            // could for the same reasons.
            throw new IllegalStateException("Could not restore driver call.")
          }
        }

        assert(signals.size() == 0)
        val signalsTag = nbt.getTagList("signals")
        signals.addAll((0 until signalsTag.tagCount()).
          map(signalsTag.tagAt(_).asInstanceOf[NBTTagCompound]).
          map(signal => {
            val argsTag = signal.getCompoundTag("args")
            val argsLength = argsTag.getInteger("length")
            new Signal(signal.getString("name"),
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

        timeStarted = nbt.getLong("timeStarted")

        // Start running our worker thread.
        assert(future == null)
        future = Executor.pool.submit(this)
      }
      catch {
        case t: Throwable => {
          t.printStackTrace()
          // TODO display error in-game on monitor or something
          //signal("crash", "memory corruption")
          close()
        }
      }
      finally if (lua != null) {
        // Clean up some after we're done and limit memory again.
        lua.gc(LuaState.GcAction.COLLECT, 0)
        lua.setTotalMemory(memory)
      }
    }
  }

  def writeToNBT(nbt: NBTTagCompound): Unit = this.synchronized {
    stateMonitor.synchronized {
      assert(state != State.Running) // Lock on 'this' should guarantee this.
      assert(state != State.Stopping) // Only set while executor is running.
    }

    nbt.setInteger("state", state.id)
    if (state == State.Stopped) {
      return
    }

    // Unlimit memory while persisting.
    val memory = lua.getTotalMemory()
    lua.setTotalMemory(Integer.MAX_VALUE)
    try {
      // Try persisting Lua, because that's what all of the rest depends on.
      // While in a driver call we have one object on the global stack: either
      // the function to call the driver with, or the result of the call.
      if (state == State.DriverCall || state == State.DriverReturn) {
        assert(
          if (state == State.DriverCall) lua.`type`(2) == LuaType.FUNCTION
          else lua.`type`(2) == LuaType.TABLE)
        nbt.setByteArray("stack", persist())
      }
      // Save the kernel state (which is always at stack index one).
      assert(lua.`type`(1) == LuaType.THREAD)
      nbt.setByteArray("kernel", persist())

      val list = new NBTTagList()
      for (s <- signals.iterator()) {
        val signal = new NBTTagCompound()
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

      nbt.setLong("timeStarted", timeStarted)
    }
    catch {
      case t: Throwable => {
        t.printStackTrace()
        nbt.setInteger("state", State.Stopped.id)
      }
    }
    finally {
      // Clean up some after we're done and limit memory again.
      lua.gc(LuaState.GcAction.COLLECT, 0)
      lua.setTotalMemory(memory)
    }
  }

  private def persist(): Array[Byte] = {
    lua.getGlobal("persist") // ... obj persist?
    if (lua.`type`(-1) == LuaType.FUNCTION) { // ... obj persist
      lua.pushValue(-2) // ... obj persist obj
      lua.call(1, 1) // ... obj str?
      if (lua.`type`(-1) == LuaType.STRING) { // ... obj str
        val result = lua.toByteArray(-1)
        lua.pop(1) // ... obj
        return result
      } // ... obj :(
    } // ... obj :(
    lua.pop(1) // ... obj
    return Array[Byte]()
  }

  private def unpersist(value: Array[Byte]): Boolean = {
    lua.getGlobal("unpersist") // ... unpersist?
    if (lua.`type`(-1) == LuaType.FUNCTION) { // ... unpersist
      lua.pushByteArray(value) // ... unpersist str
      lua.call(1, 1) // ... obj
      return true
    } // ... :(
    return false
  }

  def init(): Boolean = {
    // Creates a new state with all base libraries and the persistence library
    // loaded into it. This means the state has much more power than it
    // rightfully should have, so we sandbox it a bit in the following.
    lua = LuaStateFactory.createState()

    // If something went wrong while creating the state there's nothing else
    // we can do here...
    if (lua == null) return false

    try {
      // Push a couple of functions that override original Lua API functions or
      // that add new functionality to it.1)
      lua.getGlobal("os")

      // Return ingame time for os.time().
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          // Minecraft starts days at 6 o'clock, so we add those six hours.
          lua.pushNumber(worldTime + 6000)
          return 1
        }
      })
      lua.setField(-2, "time")

      // Custom os.clock() implementation returning the time the computer has
      // been running, instead of the native library...
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          // World time is in ticks, and each second has 20 ticks. Since we
          // want os.clock() to return real seconds, though, we'll divide it
          // accordingly.
          lua.pushNumber((owner.world.getTotalWorldTime() - timeStarted) / 20.0)
          return 1
        }
      })
      lua.setField(-2, "clock")

      // Custom os.difftime(). For most Lua implementations this would be the
      // same anyway, but just to be on the safe side.
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          val t2 = lua.checkNumber(1)
          val t1 = lua.checkNumber(2)
          lua.pushNumber(t2 - t1)
          return 1
        }
      })
      lua.setField(-2, "difftime")

      // Allow the system to read how much memory it uses and has available.
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          lua.pushInteger(lua.getTotalMemory() - kernelMemory)
          return 1
        }
      })
      lua.setField(-2, "totalMemory")
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          lua.pushInteger(lua.getFreeMemory())
          return 1
        }
      })
      lua.setField(-2, "freeMemory")

      // Pop the os table.
      lua.pop(1)

      lua.getGlobal("math")

      // We give each Lua state it's own randomizer, since otherwise they'd
      // use the good old rand() from C. Which can be terrible, and isn't
      // necessarily thread-safe.
      val random = new Random
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          lua.getTop() match {
            case 0 => lua.pushNumber(random.nextDouble)
            case 1 => {
              val u = lua.checkInteger(1)
              lua.checkArg(1, 1 < u, "interval is empty")
              lua.pushInteger(1 + random.nextInt(u))
            }
            case 2 => {
              val l = lua.checkInteger(1)
              val u = lua.checkInteger(2)
              lua.checkArg(1, l < u, "interval is empty")
              lua.pushInteger(l + random.nextInt(u - l))
            }
            case _ => throw new IllegalArgumentException("wrong number of arguments")
          }
          return 1
        }
      })
      lua.setField(-2, "random")

      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          val seed = lua.checkInteger(1)
          random.setSeed(seed)
          return 0
        }
      })
      lua.setField(-2, "randomseed")

      // Pop the math table.
      lua.pop(1)

      // Until we get to ingame screens we log to Java's stdout.
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          for (i <- 1 to lua.getTop()) {
            lua.`type`(i) match {
              case LuaType.NIL => print("nil")
              case LuaType.BOOLEAN => print(lua.toBoolean(i))
              case LuaType.NUMBER => print(lua.toNumber(i))
              case LuaType.STRING => print(lua.toString(i))
              case LuaType.TABLE => print("table")
              case LuaType.FUNCTION => print("function")
              case LuaType.THREAD => print("thread")
              case LuaType.LIGHTUSERDATA | LuaType.USERDATA => print("userdata")
            }
          }
          return 0
        }
      })
      lua.setGlobal("print")

      // TODO Other overrides?

      // Install all driver callbacks into the state. This is done once in
      // the beginning so that we can take the memory the callbacks use into
      // account when computing the kernel's memory use, as well as for
      // building a table of permanent values used when persisting/unpersisting
      // the state.
      lua.newTable()
      lua.setGlobal("drivers")
      Drivers.injectInto(this)

      // Run the boot script. This creates the global sandbox variable that is
      // used as the environment for any processes the kernel spawns, adds a
      // couple of library functions and sets up the permanent value tables as
      // well as making the functions used for persisting/unpersisting
      // available as globals.
      lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/boot.lua"), "boot", "t")
      lua.call(0, 0)

      // Load the basic kernel which takes care of handling signals by managing
      // the list of active processes. Whatever functionality we can we
      // implement in Lua, so we also implement most of the kernel's
      // functionality in Lua. Why? Because like this it's automatically
      // persisted for us without having to write more additional NBT stuff.
      lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/kernel.lua"), "kernel", "t")
      lua.newThread() // Leave it as the first value on the stack.

      // Run the garbage collector to get rid of stuff left behind after the
      // initialization phase to get a good estimate of the base memory usage
      // the kernel has. We remember that size to grant user-space programs a
      // fixed base amount of memory, regardless of the memory need of the
      // underlying system (which may change across releases).
      lua.gc(LuaState.GcAction.COLLECT, 0)
      kernelMemory = lua.getTotalMemory() - lua.getFreeMemory()
      lua.setTotalMemory(kernelMemory + 64 * 1024)

      println("Kernel uses " + (kernelMemory / 1024) + "KB of memory.")

      // Clear any left-over signals from a previous run.
      signals.clear()

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

  def close(): Unit = stateMonitor.synchronized(
    if (state != State.Stopped) {
      state = State.Stopped
      lua.setTotalMemory(Integer.MAX_VALUE);
      lua.close()
      lua = null
      kernelMemory = 0
      signals.clear()
      timeStarted = 0
      future = null
    })

  // This is a really high level lock that we only use for saving and loading.
  def run(): Unit = this.synchronized {
    println(" > executor enter")

    val driverReturn = State.DriverReturn == stateMonitor.synchronized {
      val oldState = state
      state = State.Running
      oldState
    }

    try {
      // See if the game appears to be paused, in which case we also pause.
      if (System.currentTimeMillis - lastUpdate > 500)
        pauseMonitor.synchronized(pauseMonitor.wait())

      // This is synchronized so that we don't run a Lua state while saving or
      // loading the computer to or from an NBTTagCompound or other stuff
      // corrupting our Lua state.

      // The kernel thread will always be at stack index one.
      assert(lua.`type`(1) == LuaType.THREAD)

      // Resume the Lua state and remember the number of results we get.
      val results = if (driverReturn) {
        // If we were doing a driver call, continue where we left off.
        assert(lua.getTop() == 2)
        lua.resume(1, 1)
      }
      else signals.poll() match {
        // No signal, just run any non-sleeping processes.
        case null => lua.resume(1, 0)

        // Got a signal, inject it and call any handlers (if any).
        case signal => {
          lua.pushString(signal.name)
          signal.args.foreach {
            case arg: Byte => lua.pushInteger(arg)
            case arg: Short => lua.pushInteger(arg)
            case arg: Int => lua.pushInteger(arg)
            case arg: Long => lua.pushNumber(arg)
            case arg: Float => lua.pushNumber(arg)
            case arg: Double => lua.pushNumber(arg)
            case arg: String => lua.pushString(arg)
          }
          lua.resume(1, 1 + signal.args.length)
        }
      }

      // State has inevitably changed, mark as changed to save again.
      owner.markAsChanged()

      // Only queue for next execution step if the kernel is still alive.
      if (lua.status(1) == LuaState.YIELD) {
        // Lua state yielded normally, see what we have.
        stateMonitor.synchronized {
          if (state == State.Stopping) {
            // Someone called stop() in the meantime.
            close()
          }
          else if (results == 1 && lua.isNumber(2)) {
            // We got a number. This tells us how long we should wait before
            // resuming the state again.
            val sleep = (lua.toNumber(2) * 1000).toLong
            lua.pop(results)
            state = State.Sleeping
            future = Executor.pool.schedule(this, sleep, TimeUnit.MILLISECONDS)
          }
          else if (results == 1 && lua.isFunction(2)) {
            // If we get one function it's a wrapper for a driver call.
            state = State.DriverCall
            future = null
          }
          else {
            // Something else, just pop the results and try again.
            lua.pop(results)
            state = State.Suspended
            future = Executor.pool.submit(this)
          }
        }

        println(" < executor leave")

        // Avoid getting to the closing part after the exception handling.
        return
      }
    }
    catch {
      // The kernel should never throw. If it does, the computer crashed
      // hard, so we just close the state.
      // TODO Print something to an in-game screen, a la kernel panic.
      case ex: LuaRuntimeException => ex.printLuaStackTrace()
      case er: LuaMemoryAllocationException => {
        // This is pretty likely to happen for non-upgraded computers.
        // TODO Print an error message to an in-game screen.
        println("Out of memory!")
        er.printStackTrace()
      }
      // Top-level catch-anything, because otherwise those exceptions get
      // gobbled up by the executor unless we call the future's get().
      case t: Throwable => t.printStackTrace()
    }

    // If we come here there was an error or we stopped, kill off the state.
    close()

    println(" < executor leave")
  }

  /** Signals are messages sent to the Lua state from Java asynchronously. */
  private class Signal(val name: String, val args: Array[Any])

  /** Possible states of the computer, and in particular its executor. */
  private object State extends Enumeration {
    /** The computer is not running right now and there is no Lua state. */
    val Stopped = Value("Stopped")

    /** The computer is running but yielded for a moment. */
    val Suspended = Value("Suspended")

    /** The computer is running but yielding for a longer amount of time. */
    val Sleeping = Value("Sleeping")

    /** The computer is up and running, executing Lua code. */
    val Running = Value("Running")

    /** The computer is currently shutting down (waiting for executor). */
    val Stopping = Value("Stopping")

    /** The computer executor is waiting for a driver call to be made. */
    val DriverCall = Value("DriverCall")

    /** The computer should resume with the result of a driver call. */
    val DriverReturn = Value("DriverReturn")
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
        val thread = new Thread(group, r, name)
        if (!thread.isDaemon())
          thread.setDaemon(true)
        if (thread.getPriority() != Thread.MIN_PRIORITY)
          thread.setPriority(Thread.MIN_PRIORITY)
        return thread
      }
    })
  }
}
