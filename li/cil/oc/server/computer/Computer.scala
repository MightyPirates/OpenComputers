package li.cil.oc.server.computer

import com.naef.jnlua._
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import li.cil.oc.common.computer.IComputer
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.nbt._
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import scala.Array.canBuildFrom
import scala.Some
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.io.Source

/**
 * Wrapper class for Lua states set up to behave like a pseudo-OS.
 * <p/>
 * This class takes care of the following:
 * <ul>
 * <li>Creating a new Lua state when started from a previously stopped state.</li>
 * <li>Updating the Lua state in a parallel thread so as not to block the game.</li>
 * <li>Synchronizing calls from the computer thread to the network.</li>
 * <li>Saving the internal state of the computer across chunk saves/loads.</li>
 * <li>Closing the Lua state when stopping a previously running computer.</li>
 * </ul>
 * <p/>
 * See `Driver` to read more about component drivers and how they interact
 * with computers - and through them the components they interface.
 */
class Computer(val owner: IComputerEnvironment) extends IComputer with Runnable {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  /**
   * The current execution state of the computer. This is used to track how to
   * resume the computers main thread, if at all, and whether to accept new
   * signals or not.
   */
  private var state = Computer.State.Stopped

  /** The internal Lua state. Only set while the computer is running. */
  private[computer] var lua: LuaState = null

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
   * means to communicate actively with the computer (passively only message
   * handlers can interact with the computer by returning some result).
   */
  private val signals = new LinkedBlockingQueue[Computer.Signal](100)

  // ----------------------------------------------------------------------- //

  /**
   * The time (world time) when the computer was started. This is used for our
   * custom implementation of os.clock(), which returns the amount of the time
   * the computer has been running.
   */
  private var timeStarted = 0.0

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
  private var future: Option[Future[_]] = None

  /** This is used to synchronize access to the state field. */
  private val stateMonitor = new Object()

  /** This is used to synchronize while saving, so we don't stop while we do. */
  private val saveMonitor = new Object()

  // ----------------------------------------------------------------------- //
  // IComputerContext
  // ----------------------------------------------------------------------- //

  override def signal(name: String, args: Any*) = {
    def values = args.map {
      case null | Unit => Unit
      case arg: Boolean => arg
      case arg: Byte => arg.toDouble
      case arg: Char => arg.toDouble
      case arg: Short => arg.toDouble
      case arg: Int => arg.toDouble
      case arg: Long => arg.toDouble
      case arg: Float => arg.toDouble
      case arg: Double => arg
      case arg: String => arg
      case _ => throw new IllegalArgumentException()
    }.toArray
    stateMonitor.synchronized(state match {
      // We don't push new signals when stopped or shutting down.
      case Computer.State.Stopped | Computer.State.Stopping => false
      // Currently sleeping. Cancel that and start immediately.
      case Computer.State.Sleeping =>
        val v = values // Map first, may error.
        future.get.cancel(true)
        state = Computer.State.Suspended
        signals.offer(new Computer.Signal(name, v))
        future = Some(Computer.Executor.pool.submit(this))
        true
      // Basically running, but had nothing to do so we stopped. Resume.
      case Computer.State.Suspended if !future.isDefined =>
        signals.offer(new Computer.Signal(name, values))
        future = Some(Computer.Executor.pool.submit(this))
        true
      // Running or in synchronized call, just push the signal.
      case _ =>
        signals.offer(new Computer.Signal(name, values))
        true
    })
  }

  // ----------------------------------------------------------------------- //
  // IComputer
  // ----------------------------------------------------------------------- //

  override def start() = stateMonitor.synchronized(
    state == Computer.State.Stopped && init() && {
      state = Computer.State.Suspended

      // Mark state change in owner, to send it to clients.
      owner.markAsChanged()

      // Inject a dummy signal so that real ones don't get swallowed. This way
      // we can just ignore the parameters the first time the kernel is run
      // and all actual signals will be read using coroutine.yield().
      // IMPORTANT: This will also create our worker thread for the first run.
      signal("dummy")

      // Inject component added signals for all nodes in the network.
      owner.network.nodes.foreach(node => signal("component_added", node.address))
      true
    })

  override def stop() = saveMonitor.synchronized(stateMonitor.synchronized {
    if (state != Computer.State.Stopped) {
      if (state != Computer.State.Running) {
        // If the computer is not currently running we can simply close it,
        // and cancel any pending future - which may already be running and
        // waiting for the stateMonitor, so we do a hard abort.
        future.foreach(_.cancel(true))
        close()
      }
      else {
        // Otherwise we enter an intermediate state to ensure the executor
        // truly stopped, before switching back to stopped to allow starting
        // the computer again. The executor will check for this state and
        // call close.
        state = Computer.State.Stopping
      }
      true
    }
    else false
  })

  override def isRunning = stateMonitor.synchronized(state != Computer.State.Stopped)

  override def update() {
    stateMonitor.synchronized(state match {
      case Computer.State.Stopped | Computer.State.Stopping => return
      case Computer.State.SynchronizedCall => {
        assert(lua.getTop == 2)
        assert(lua.isThread(1))
        assert(lua.isFunction(2))
        try {
          lua.call(0, 1)
          lua.checkType(2, LuaType.TABLE)
          state = Computer.State.SynchronizedReturn
          assert(!future.isDefined)
          future = Some(Computer.Executor.pool.submit(this))
        } catch {
          // This can happen if we run out of memory while converting a Java exception to a string.
          case _: LuaMemoryAllocationException =>
            // TODO error message somewhere ingame
            close()
          // This should not happen.
          case _: Throwable => {
            OpenComputers.log.warning("Faulty Lua implementation for synchronized calls.")
            close()
          }
        }
      }
      case Computer.State.Paused => {
        state = Computer.State.Suspended
        assert(!future.isDefined)
        future = Some(Computer.Executor.pool.submit(this))
      }
      case Computer.State.SynchronizedReturnPaused => {
        state = Computer.State.SynchronizedReturn
        assert(!future.isDefined)
        future = Some(Computer.Executor.pool.submit(this))
      }
      case _ => /* Nothing special to do. */
    })

    // Update world time for computer threads.
    worldTime = owner.world.getWorldInfo.getWorldTotalTime

    // Remember when we started the computer for os.clock(). We do this in the
    // update because only then can we be sure the world is available.
    if (timeStarted == 0)
      timeStarted = worldTime

    // Update last time run to let our executor thread know it doesn't have to
    // pause.
    lastUpdate = System.currentTimeMillis
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound): Unit =
    saveMonitor.synchronized(this.synchronized {
      // Clear out what we currently have, if anything.
      stateMonitor.synchronized {
        assert(state != Computer.State.Running) // Lock on 'this' should guarantee this.
        stop()
      }

      state = Computer.State(nbt.getInteger("state"))

      if (state != Computer.State.Stopped && init()) {
        // Unlimit memory use while unpersisting.
        val memory = lua.getTotalMemory
        lua.setTotalMemory(Integer.MAX_VALUE)
        try {
          // Try unpersisting Lua, because that's what all of the rest depends
          // on. First, clear the stack, meaning the current kernel.
          lua.setTop(0)

          if (!unpersist(nbt.getByteArray("kernel")) || !lua.isThread(1)) {
            // This shouldn't really happen, but there's a chance it does if
            // the save was corrupt (maybe someone modified the Lua files).
            throw new IllegalStateException("Could not restore kernel.")
          }
          if (state == Computer.State.SynchronizedCall || state == Computer.State.SynchronizedReturn) {
            if (!unpersist(nbt.getByteArray("stack")) ||
              (state == Computer.State.SynchronizedCall && !lua.isFunction(2)) ||
              (state == Computer.State.SynchronizedReturn && !lua.isTable(2))) {
              // Same as with the above, should not really happen normally, but
              // could for the same reasons.
              throw new IllegalStateException("Could not restore stack.")
            }
            assert(lua.getTop == 2)
          }

          assert(signals.size() == 0)
          val signalsTag = nbt.getTagList("signals")
          signals.addAll((0 until signalsTag.tagCount()).
            map(signalsTag.tagAt(_).asInstanceOf[NBTTagCompound]).
            map(signal => {
            val argsTag = signal.getCompoundTag("args")
            val argsLength = argsTag.getInteger("length")
            new Computer.Signal(signal.getString("name"),
              (0 until argsLength).map("arg" + _).map(argsTag.getTag).map {
                case tag: NBTTagByte if tag.data == -1 => Unit
                case tag: NBTTagByte => tag.data == 1
                case tag: NBTTagDouble => tag.data
                case tag: NBTTagString => tag.data
              }.toArray)
          }).asJava)

          timeStarted = nbt.getDouble("timeStarted")

          // Clean up some after we're done and limit memory again.
          lua.gc(LuaState.GcAction.COLLECT, 0)
          lua.setTotalMemory(memory)

          // Start running our worker thread.
          assert(!future.isDefined)
          state match {
            case Computer.State.Suspended | Computer.State.Sleeping | Computer.State.SynchronizedReturn =>
              future = Some(Computer.Executor.pool.submit(this))
            case _ => // Wasn't running before.
          }

        } catch {
          case t: Throwable => {
            OpenComputers.log.log(Level.WARNING, "Could not restore computer.", t)
            close()
          }
        }
      }
      else {
        close()
      }
    })

  override def save(nbt: NBTTagCompound): Unit =
    saveMonitor.synchronized(this.synchronized {
      stateMonitor.synchronized {
        assert(state != Computer.State.Running) // Lock on 'this' should guarantee this.
        assert(state != Computer.State.Stopping) // Only set while executor is running.
      }

      nbt.setInteger("state", state.id)
      if (state == Computer.State.Stopped) {
        return
      }

      // Unlimit memory while persisting.
      val memory = lua.getTotalMemory
      lua.setTotalMemory(Integer.MAX_VALUE)
      try {
        // Try persisting Lua, because that's what all of the rest depends on.
        // While in a driver call we have one object on the global stack: either
        // the function to call the driver with, or the result of the call.
        if (state == Computer.State.SynchronizedCall || state == Computer.State.SynchronizedReturn) {
          assert(if (state == Computer.State.SynchronizedCall) lua.isFunction(2) else lua.isTable(2))
          nbt.setByteArray("stack", persist(2))
        }
        // Save the kernel state (which is always at stack index one).
        assert(lua.isThread(1))
        nbt.setByteArray("kernel", persist(1))

        val list = new NBTTagList
        for (s <- signals.iterator) {
          val signal = new NBTTagCompound
          signal.setString("name", s.name)
          val args = new NBTTagCompound
          args.setInteger("length", s.args.length)
          s.args.zipWithIndex.foreach {
            case (Unit, i) => args.setByte("arg" + i, -1)
            case (arg: Boolean, i) => args.setByte("arg" + i, if (arg) 1 else 0)
            case (arg: Double, i) => args.setDouble("arg" + i, arg)
            case (arg: String, i) => args.setString("arg" + i, arg)
          }
          signal.setCompoundTag("args", args)
          list.appendTag(signal)
        }
        nbt.setTag("signals", list)

        nbt.setDouble("timeStarted", timeStarted)
      }
      catch {
        case t: Throwable => {
          t.printStackTrace()
          nbt.setInteger("state", Computer.State.Stopped.id)
        }
      }
      finally {
        // Clean up some after we're done and limit memory again.
        lua.gc(LuaState.GcAction.COLLECT, 0)
        lua.setTotalMemory(memory)
      }
    })

  private def persist(index: Int): Array[Byte] = {
    lua.getGlobal("persist") // ... obj persist?
    if (lua.isFunction(-1)) {
      // ... obj persist
      lua.pushValue(index) // ... obj persist obj
      lua.call(1, 1) // ... obj str?
      if (lua.isString(-1)) {
        // ... obj str
        val result = lua.toByteArray(-1)
        lua.pop(1) // ... obj
        return result
      } // ... obj :(
    } // ... obj :(
    lua.pop(1) // ... obj
    Array[Byte]()
  }

  private def unpersist(value: Array[Byte]): Boolean = {
    lua.getGlobal("unpersist") // ... unpersist?
    if (lua.isFunction(-1)) {
      // ... unpersist
      lua.pushByteArray(value) // ... unpersist str
      lua.call(1, 1) // ... obj
      return true
    } // ... :(
    false
  }

  // ----------------------------------------------------------------------- //
  // Internals
  // ----------------------------------------------------------------------- //

  private def init(): Boolean = {
    // Creates a new state with all base libraries and the persistence library
    // loaded into it. This means the state has much more power than it
    // rightfully should have, so we sandbox it a bit in the following.
    LuaStateFactory.createState() match {
      case None =>
        lua = null
        return false
      case Some(value) => lua = value
    }

    try {
      // Push a couple of functions that override original Lua API functions or
      // that add new functionality to it.

      // Push a couple of functions that override original Lua API functions or
      // that add new functionality to it.
      lua.getGlobal("os")

      // Custom os.clock() implementation returning the time the computer has
      // been running, instead of the native library...
      lua.pushJavaFunction(ScalaFunction(lua => {
        // World time is in ticks, and each second has 20 ticks. Since we
        // want os.clock() to return real seconds, though, we'll divide it
        // accordingly.
        lua.pushNumber((worldTime - timeStarted) / 20.0)
        1
      }))
      lua.setField(-2, "clock")

      // Return ingame time for os.time().
      lua.pushJavaFunction(ScalaFunction(lua => {
        // Game time is in ticks, so that each day has 24000 ticks, meaning
        // one hour is game time divided by one thousand. Also, Minecraft
        // starts days at 6 o'clock, so we add those six hours. Thus:
        // timestamp = (time + 6000) / 1000[h] * 60[m] * 60[s] * 1000[ms]
        lua.pushNumber((worldTime + 6000) * 60 * 60)
        1
      }))
      lua.setField(-2, "time")

      // Allow the system to read how much memory it uses and has available.
      lua.pushJavaFunction(ScalaFunction(lua => {
        lua.pushInteger(kernelMemory)
        1
      }))
      lua.setField(-2, "romSize")

      // Allow the computer to figure out its own id in the component network.
      lua.pushJavaFunction(ScalaFunction(lua => {
        lua.pushInteger(owner.address)
        1
      }))
      lua.setField(-2, "address")

      // Pop the os table.
      lua.pop(1)

      // Until we get to ingame screens we log to Java's stdout.
      lua.pushJavaFunction(ScalaFunction(lua => {
        for (i <- 1 to lua.getTop) lua.`type`(i) match {
          case LuaType.NIL => print("nil")
          case LuaType.BOOLEAN => print(lua.toBoolean(i))
          case LuaType.NUMBER => print(lua.toNumber(i))
          case LuaType.STRING => print(lua.toString(i))
          case LuaType.TABLE => print("table")
          case LuaType.FUNCTION => print("function")
          case LuaType.THREAD => print("thread")
          case LuaType.LIGHTUSERDATA | LuaType.USERDATA => print("userdata")
        }
        println()
        0
      }))
      lua.setGlobal("print")

      // Set up functions used to send network messages.
      def parseArgument(lua: LuaState, index: Int) = lua.`type`(index) match {
        case LuaType.BOOLEAN => lua.toBoolean(index)
        case LuaType.NUMBER => lua.toNumber(index)
        case LuaType.STRING => lua.toString(index)
        case _ => Unit
      }

      def parseArguments(lua: LuaState, start: Int) =
        for (index <- start to lua.getTop) yield parseArgument(lua, index)

      def pushResult(lua: LuaState, value: Any): Unit = value match {
        case value: Boolean => lua.pushBoolean(value)
        case value: Byte => lua.pushNumber(value)
        case value: Short => lua.pushNumber(value)
        case value: Int => lua.pushNumber(value)
        case value: Long => lua.pushNumber(value)
        case value: Float => lua.pushNumber(value)
        case value: Double => lua.pushNumber(value)
        case value: String => lua.pushString(value)
        case value: Array[_] => {
          lua.newTable()
          value.zipWithIndex.foreach {
            case (entry, index) =>
              pushResult(lua, entry)
              lua.rawSet(-2, index)
          }
        }
        // TODO maps, tuples/seqs?
        // TODO I fear they are, but check if the following are really necessary for Java interop.
        case value: java.lang.Byte => lua.pushNumber(value.byteValue)
        case value: java.lang.Short => lua.pushNumber(value.shortValue)
        case value: java.lang.Integer => lua.pushNumber(value.intValue)
        case value: java.lang.Long => lua.pushNumber(value.longValue)
        case value: java.lang.Float => lua.pushNumber(value.floatValue)
        case value: java.lang.Double => lua.pushNumber(value.doubleValue)
        case _ => lua.pushNil()
      }

      lua.pushJavaFunction(ScalaFunction(lua =>
        owner.network.sendToNode(owner, lua.checkInteger(1), lua.checkString(2), parseArguments(lua, 3): _*) match {
          case Some(Array(results@_*)) =>
            results.foreach(pushResult(lua, _))
            results.length
          case None => 0
        }))
      lua.setGlobal("sendToNode")

      lua.pushJavaFunction(ScalaFunction(lua => {
        owner.network.sendToAll(owner, lua.checkString(1), parseArguments(lua, 2): _*)
        0
      }))
      lua.setGlobal("sendToAll")

      lua.pushJavaFunction(ScalaFunction(lua => {
        owner.network.node(lua.checkInteger(1)) match {
          case None => 0
          case Some(node) => lua.pushString(node.name); 1
        }
      }))
      lua.setGlobal("nodeName")

      // Run the boot script. This sets up the permanent value tables as
      // well as making the functions used for persisting/unpersisting
      // available as globals. It also wraps the message sending functions
      // so that they yield a closure doing the actual call so that that
      // message call can be performed in a synchronized fashion.
      lua.load(classOf[Computer].getResourceAsStream(
        "/assets/opencomputers/lua/boot.lua"), "=boot", "t")
      lua.call(0, 0)

      // Install all driver callbacks into the state. This is done once in
      // the beginning so that we can take the memory the callbacks use into
      // account when computing the kernel's memory use.
      Drivers.installOn(this)

      // Loads the init script. This is run by the kernel as a separate
      // coroutine to enforce timeouts and sandbox user scripts.
      lua.pushJavaFunction(new JavaFunction() {
        def invoke(lua: LuaState): Int = {
          lua.pushString(Source.fromInputStream(classOf[Computer].
            getResourceAsStream("/assets/opencomputers/lua/init.lua")).mkString)
          1
        }
      })
      lua.setGlobal("init")

      // Load the basic kernel which takes care of handling signals by managing
      // the list of active processes. Whatever functionality we can we
      // implement in Lua, so we also implement most of the kernel's
      // functionality in Lua. Why? Because like this it's automatically
      // persisted for us without having to write more additional NBT stuff.
      lua.load(classOf[Computer].getResourceAsStream(
        "/assets/opencomputers/lua/kernel.lua"), "=kernel", "t")
      lua.newThread() // Leave it as the first value on the stack.

      // Run the garbage collector to get rid of stuff left behind after the
      // initialization phase to get a good estimate of the base memory usage
      // the kernel has. We remember that size to grant user-space programs a
      // fixed base amount of memory, regardless of the memory need of the
      // underlying system (which may change across releases).
      lua.gc(LuaState.GcAction.COLLECT, 0)
      kernelMemory = lua.getTotalMemory - lua.getFreeMemory
      lua.setTotalMemory(kernelMemory + 64 * 1024)

      // Clear any left-over signals from a previous run.
      signals.clear()

      return true
    }
    catch {
      case ex: Throwable => {
        OpenComputers.log.log(Level.WARNING, "Failed initializing computer.", ex)
        close()
      }
    }
    false
  }

  private def close(): Unit = stateMonitor.synchronized(
    if (state != Computer.State.Stopped) {
      state = Computer.State.Stopped
      lua.setTotalMemory(Integer.MAX_VALUE)
      lua.close()
      lua = null
      kernelMemory = 0
      signals.clear()
      timeStarted = 0
      future = None

      // Mark state change in owner, to send it to clients.
      owner.markAsChanged()
    })

  // This is a really high level lock that we only use for saving and loading.
  override def run(): Unit = this.synchronized {
    // See if the game appears to be paused, in which case we also pause.
    if (System.currentTimeMillis - lastUpdate > 200)
      stateMonitor.synchronized {
        state =
          if (state == Computer.State.SynchronizedReturn) Computer.State.SynchronizedReturnPaused
          else Computer.State.Paused
        future = None
        return
      }

    val callReturn = stateMonitor.synchronized {
      if (state == Computer.State.Stopped) return
      val oldState = state
      state = Computer.State.Running
      future = None
      oldState
    } match {
      case Computer.State.SynchronizedReturn | Computer.State.SynchronizedReturnPaused => true
      case Computer.State.Stopped | Computer.State.Paused | Computer.State.Suspended | Computer.State.Sleeping => false
      case s =>
        OpenComputers.log.warning("Running computer from invalid state " + s.toString + "!")
        stateMonitor.synchronized {
          state = s
          future = None
        }
        return
    }

    try {
      // This is synchronized so that we don't run a Lua state while saving or
      // loading the computer to or from an NBTTagCompound or other stuff
      // corrupting our Lua state.

      // The kernel thread will always be at stack index one.
      assert(lua.isThread(1))

      // Resume the Lua state and remember the number of results we get.
      val results = if (callReturn) {
        // If we were doing a driver call, continue where we left off.
        assert(lua.getTop == 2)
        lua.resume(1, 1)
      }
      else signals.poll() match {
        // No signal, just run any non-sleeping processes.
        case null => lua.resume(1, 0)

        // Got a signal, inject it and call any handlers (if any).
        case signal => {
          lua.pushString(signal.name)
          signal.args.foreach {
            case Unit => lua.pushNil()
            case arg: Boolean => lua.pushBoolean(arg)
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
          if (state == Computer.State.Stopping) {
            // Someone called stop() in the meantime.
            close()
          }
          else if (results == 1 && lua.isNumber(2)) {
            // We got a number. This tells us how long we should wait before
            // resuming the state again.
            val sleep = (lua.toNumber(2) * 1000).toLong
            lua.pop(results)
            if (signals.isEmpty) {
              state = Computer.State.Sleeping
              assert(!future.isDefined)
              future = Some(Computer.Executor.pool.schedule(this, sleep, TimeUnit.MILLISECONDS))
            }
            else {
              state = Computer.State.Suspended
              assert(!future.isDefined)
              future = Some(Computer.Executor.pool.submit(this))
            }
          }
          else if (results == 1 && lua.isFunction(2)) {
            // If we get one function it's a wrapper for a synchronized call.
            state = Computer.State.SynchronizedCall
            assert(!future.isDefined)
          }
          else {
            // Something else, just pop the results and try again.
            lua.pop(results)
            state = Computer.State.Suspended
            assert(!future.isDefined)
            if (!signals.isEmpty) future = Some(Computer.Executor.pool.submit(this))
          }
        }

        // Avoid getting to the closing part after the exception handling.
        return
      }
      // Error handling.
      else if (lua.isBoolean(2) && !lua.toBoolean(2)) {
        // TODO Print something to an in-game screen.
        OpenComputers.log.warning(lua.toString(3))
      }
    }
    catch {
      case er: LuaMemoryAllocationException => {
        // This is pretty likely to happen for non-upgraded computers.
        // TODO Print something to an in-game screen, a la kernel panic.
        OpenComputers.log.warning("Out of memory!")
      }
      // Top-level catch-anything, because otherwise those exceptions get
      // gobbled up by the executor unless we call the future's get().
      case t: Throwable =>
        OpenComputers.log.warning("Faulty kernel implementation, it should never throw.")
    }

    // If we come here there was an error or we stopped, kill off the state.
    close()
  }
}

object Computer {
  @ForgeSubscribe
  def onChunkUnload(e: ChunkEvent.Unload) =
    onUnload(e.world, e.getChunk.chunkTileEntityMap.values.map(_.asInstanceOf[TileEntity]))

  private def onUnload(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    tileEntities.
      filter(_.isInstanceOf[TileEntityComputer]).
      map(_.asInstanceOf[TileEntityComputer]).
      foreach(_.turnOff())
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

    /** The computer is paused and waiting for the game to resume. */
    val Paused = Value("Paused")

    /** The computer is up and running, executing Lua code. */
    val Running = Value("Running")

    /** The computer is currently shutting down (waiting for executor). */
    val Stopping = Value("Stopping")

    /** The computer executor is waiting for a synchronized call to be made. */
    val SynchronizedCall = Value("SynchronizedCall")

    /** The computer should resume with the result of a synchronized call. */
    val SynchronizedReturn = Value("SynchronizedReturn")

    /** The computer is paused and waiting for the game to resume. */
    val SynchronizedReturnPaused = Value("SynchronizedReturnPaused")
  }

  /** Singleton for requesting executors that run our Lua states. */
  private object Executor {
    val pool = Executors.newScheduledThreadPool(Config.threads,
      new ThreadFactory() {
        private val threadNumber = new AtomicInteger(1)

        private val group = System.getSecurityManager match {
          case null => Thread.currentThread().getThreadGroup
          case s => s.getThreadGroup
        }

        def newThread(r: Runnable): Thread = {
          val name = "OpenComputers-" + threadNumber.getAndIncrement
          val thread = new Thread(group, r, name)
          if (!thread.isDaemon)
            thread.setDaemon(true)
          if (thread.getPriority != Thread.MIN_PRIORITY)
            thread.setPriority(Thread.MIN_PRIORITY)
          thread
        }
      })
  }

}