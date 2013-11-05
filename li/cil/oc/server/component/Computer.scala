package li.cil.oc.server.component

import com.naef.jnlua._
import java.io.{FileNotFoundException, IOException}
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import li.cil.oc
import li.cil.oc.api
import li.cil.oc.api.Persistable
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity
import li.cil.oc.server
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.nbt._
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import scala.Array.canBuildFrom
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.math.ScalaNumber

/**
 * Wrapper class for Lua states set up to behave like a pseudo-OS.
 * <p/>
 * This class takes care of the following:
 * <ul>
 * <li>Creating a new Lua state when started from a previously stopped state.</li>
 * <li>Updating the Lua state in a parallel thread so as not to block the game.</li>
 * <li>Synchronizing calls from the computer thread to other game components.</li>
 * <li>Saving the internal state of the computer across chunk saves/loads.</li>
 * <li>Closing the Lua state when stopping a previously running computer.</li>
 * </ul>
 * <p/>
 * Computers are relatively useless without drivers. Drivers are a combination
 * of Lua and Java/Scala code that allows the computer to interact with the
 * game world, or more specifically: with other components, by sending messages
 * across the component network the computer is connected to (see `Network`).
 * <p/>
 * Host code (Java/Scala) cannot directly call Lua code. It can only queue
 * signals (events, messages, packets, whatever you want to call it) which will
 * be passed to the Lua state one by one and processed there (see `signal`).
 * <p/>
 *
 */
class Computer(val owner: Computer.Environment) extends Persistable with Runnable {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  private var state = Computer.State.Stopped

  private val stateMonitor = new Object() // To synchronize access to `state`.

  private var future: Option[Future[_]] = None

  private var lua: LuaState = null

  private var kernelMemory = 0

  private val components = mutable.Map.empty[String, String]

  private val addedComponents = mutable.Set.empty[Component]

  private val signals = new mutable.Queue[Computer.Signal]

  private val rom = Option(api.FileSystem.
    fromClass(OpenComputers.getClass, Config.resourceDomain, "lua/rom")).
    flatMap(fs => Option(api.FileSystem.asManagedEnvironment(fs, "rom")))

  private val tmp = Option(api.FileSystem.
    fromMemory(512 * 1024)).
    flatMap(fs => Option(api.FileSystem.asManagedEnvironment(fs, "tmpfs")))

  // ----------------------------------------------------------------------- //

  private var timeStarted = 0L // Game-world time [ms] for os.uptime().

  private var worldTime = 0L // Game-world time for os.time().

  private var lastUpdate = 0L // Real-world time [ms] for pause detection.

  private var cpuTime = 0L // Pseudo-real-world time [ns] for os.clock().

  private var cpuStart = 0L // Pseudo-real-world time [ns] for os.clock().

  private var sleepUntil = 0.0 // Real-world time [ms].

  private var wasRunning = false // To signal stops synchronously.

  private var message: Option[String] = None // For error messages.

  // ----------------------------------------------------------------------- //

  def recomputeMemory() = stateMonitor.synchronized(if (lua != null) {
    lua.setTotalMemory(Int.MaxValue)
    lua.gc(LuaState.GcAction.COLLECT, 0)
    if (kernelMemory > 0)
      lua.setTotalMemory(kernelMemory + owner.installedMemory)
  })

  // ----------------------------------------------------------------------- //

  def start() = stateMonitor.synchronized(owner.node.network != null &&
    state == Computer.State.Stopped &&
    owner.installedMemory > 0 &&
    init() && {
    // Initial state. Will be switched to State.Yielded in the next update()
    // due to the signals queue not being empty (
    state = Computer.State.Suspended

    // Remember when we started, for os.clock().
    timeStarted = owner.world.getWorldInfo.getWorldTotalTime

    // Mark state change in owner, to send it to clients.
    owner.markAsChanged(8) // Initial power required to start.

    // Push a dummy signal to get the computer going.
    signal("dummy")

    // All green, computer started successfully.
    owner.node.sendToReachable("computer.started")
    true
  })

  def stop() = stateMonitor.synchronized(state match {
    case Computer.State.Stopped => false // Nothing to do.
    case _ if future.isEmpty => close(); true // Not executing, kill it.
    case _ =>
      // If the computer is currently executing something we enter an
      // intermediate state to ensure the executor or synchronized call truly
      // stopped, before switching back to stopped to allow starting the
      // computer again. The executor and synchronized call will check for
      // this state and call close(), thus switching the state to stopped.
      state = Computer.State.Stopping
      true
  })

  def isRunning = state != Computer.State.Stopped

  // ----------------------------------------------------------------------- //

  def signal(name: String, args: Any*) = stateMonitor.synchronized(state match {
    case Computer.State.Stopped | Computer.State.Stopping => false
    case _ if signals.size >= 256 => false
    case _ =>
      signals.enqueue(new Computer.Signal(name, args.map {
        case null | Unit | None => Unit
        case arg: java.lang.Boolean => arg
        case arg: java.lang.Byte => arg.toDouble
        case arg: java.lang.Character => arg.toDouble
        case arg: java.lang.Short => arg.toDouble
        case arg: java.lang.Integer => arg.toDouble
        case arg: java.lang.Long => arg.toDouble
        case arg: java.lang.Float => arg.toDouble
        case arg: java.lang.Double => arg
        case arg: java.lang.String => arg
        case arg: Array[Byte] => arg
        case arg =>
          OpenComputers.log.warning("Trying to push signal with an unsupported argument of type " + arg.getClass.getName)
          Unit
      }.toArray))
      true
  })

  def addComponent(component: Component) {
    if (!components.contains(component.address)) {
      addedComponents += component
    }
  }

  def removeComponent(component: Component) {
    if (components.contains(component.address)) {
      components -= component.address
      signal("component_removed", component.address, component.name)
    }
    addedComponents -= component
  }

  private def processAddedComponents() {
    for (component <- addedComponents) {
      if (component.canBeSeenFrom(owner.node)) {
        components += component.address -> component.name
        // Skip the signal if we're not initialized yet, since we'd generate a
        // duplicate in the startup script otherwise.
        if (kernelMemory > 0)
          signal("component_added", component.address, component.name)
      }
    }
    addedComponents.clear()
  }

  private def verifyComponents() {
    val invalid = mutable.Set.empty[String]
    for ((address, name) <- components) {
      if (owner.node.network.node(address) == null) {
        OpenComputers.log.warning("A component of type '" + name +
          "' disappeared! This usually means that it didn't save its node.")
        signal("component_removed", address, name)
        invalid += address
      }
    }
    for (address <- invalid) {
      components -= address
    }
  }

  def update() {
    // Add components that were added since the last update to the actual list
    // of components if we can see them. We use this delayed approach to avoid
    // issues with components that have a visibility lower than their
    // reachability, because in that case if they get connected in the wrong
    // order we wouldn't add them (since they'd be invisible in their connect
    // message, and only become visible with a later node-to-node connection,
    // but that wouldn't trigger a connect message anymore due to the higher
    // reachability).
    processAddedComponents()

    // Are we waiting for the world to settle?
    if (lastUpdate == 0)
      if (System.currentTimeMillis() < sleepUntil)
        return
      else
        verifyComponents()

    // Update last time run to let our executor thread know it doesn't have to
    // pause.
    lastUpdate = System.currentTimeMillis

    // TODO This seems to be the "run time", not the elapsed ingame time. For example, when doing /time set 0 the game
    // should jump to the next day, but this value does not jump. Is this just Forge or do we have to find some other
    // way around this? CC seems to use getWorldTime, which is really odd, since that should be only within the range
    // of a single day (0 to 24000), which it *is*... perhaps vanilla Minecraft (not re-compiled) behaves different?
    // Update world time for computer threads.
    worldTime = owner.world.getWorldInfo.getWorldTotalTime

    def cleanup() {
      rom.foreach(_.node.remove())
      tmp.foreach(_.node.remove())
      owner.node.sendToReachable("computer.stopped")

      message.foreach(println) // TODO remove this at some point (add a tool that can read these error messages?)
    }

    // Signal stops to the network. This is used to close file handles, for example.
    if (wasRunning && !isRunning) {
      cleanup()
    }
    wasRunning = isRunning

    // Check if we should switch states.
    stateMonitor.synchronized(state match {
      // Computer is rebooting.
      case Computer.State.Rebooting => {
        close()
        cleanup()
        start()
      }
      // Resume from pauses based on signal underflow.
      case Computer.State.Suspended if !signals.isEmpty => execute(Computer.State.Yielded)
      case Computer.State.Sleeping if lastUpdate >= sleepUntil || !signals.isEmpty => execute(Computer.State.Yielded)
      // Resume in case we paused  because the game was paused.
      case Computer.State.Paused => execute(Computer.State.Yielded)
      case Computer.State.SynchronizedReturnPaused => execute(Computer.State.SynchronizedReturn)
      // Perform a synchronized call (message sending).
      case Computer.State.SynchronizedCall => {
        assert(future.isEmpty)
        // These three asserts are all guaranteed by run().
        assert(lua.getTop == 2)
        assert(lua.isThread(1))
        assert(lua.isFunction(2))
        // We switch into running state, since we'll behave as though the call
        // were performed from our executor thread.
        state = Computer.State.Running
        try {
          // Synchronized call protocol requires the called function to return
          // a table, which holds the results of the call, to be passed back
          // to the coroutine.yield() that triggered the call.
          lua.call(0, 1)
          lua.checkType(2, LuaType.TABLE)
          // Nothing should have been able to trigger a future.
          assert(future.isEmpty)
          // If the call lead to stop() being called we stop right now,
          // otherwise we return the result to our executor.
          if (state == Computer.State.Stopping)
            close()
          else {
            assert(state == Computer.State.Running)
            execute(Computer.State.SynchronizedReturn)
          }
        } catch {
          case _: LuaMemoryAllocationException =>
            // This can happen if we run out of memory while converting a Java
            // exception to a string (which we have to do to avoid keeping
            // userdata on the stack, which cannot be persisted).
            message = Some("not enough memory")
            close()
          case e: java.lang.Error if e.getMessage == "not enough memory" =>
            message = Some("not enough memory")
            close()
          case e: Throwable =>
            OpenComputers.log.log(Level.WARNING, "Faulty Lua implementation for synchronized calls.", e)
            message = Some("protocol error")
            close()
        }
      }
      case _ => // Nothing special to do, just avoid match errors.
    })
  }

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    val computerNbt = nbt.getCompoundTag("oc.computer")

    state = computerNbt.getInteger("state") match {
      case id if id >= 0 && id < Computer.State.maxId => Computer.State(id)
      case _ => Computer.State.Stopped
    }

    if (state != Computer.State.Stopped && init()) {
      // Unlimit memory use while unpersisting.
      lua.setTotalMemory(Integer.MAX_VALUE)

      try {
        // Try unpersisting Lua, because that's what all of the rest depends
        // on. First, clear the stack, meaning the current kernel.
        lua.setTop(0)

        if (!computerNbt.hasKey("kernel") || !unpersist(computerNbt.getByteArray("kernel")) || !lua.isThread(1)) {
          // This shouldn't really happen, but there's a chance it does if
          // the save was corrupt (maybe someone modified the Lua files).
          throw new IllegalStateException("Invalid kernel.")
        }
        if (state == Computer.State.SynchronizedCall || state == Computer.State.SynchronizedReturn) {
          if (!computerNbt.hasKey("stack") || !unpersist(computerNbt.getByteArray("stack")) ||
            (state == Computer.State.SynchronizedCall && !lua.isFunction(2)) ||
            (state == Computer.State.SynchronizedReturn && !lua.isTable(2))) {
            // Same as with the above, should not really happen normally, but
            // could for the same reasons.
            throw new IllegalStateException("Invalid stack.")
          }
        }

        val componentsNbt = computerNbt.getTagList("components")
        components ++= (0 until componentsNbt.tagCount).
          map(componentsNbt.tagAt).
          map(_.asInstanceOf[NBTTagCompound]).
          map(c => c.getString("address") -> c.getString("name"))

        val signalsNbt = computerNbt.getTagList("signals")
        signals ++= (0 until signalsNbt.tagCount).
          map(signalsNbt.tagAt).
          map(_.asInstanceOf[NBTTagCompound]).
          map(signalNbt => {
          val argsNbt = signalNbt.getCompoundTag("args")
          val argsLength = argsNbt.getInteger("length")
          new Computer.Signal(signalNbt.getString("name"),
            (0 until argsLength).map("arg" + _).map(argsNbt.getTag).map {
              case tag: NBTTagByte if tag.data == -1 => Unit
              case tag: NBTTagByte => tag.data == 1
              case tag: NBTTagDouble => tag.data
              case tag: NBTTagString => tag.data
              case tag: NBTTagByteArray => tag.byteArray
              case _ => Unit
            }.toArray)
        })

        rom.foreach(rom => {
          val romNbt = computerNbt.getCompoundTag("rom")
          rom.node.load(romNbt)
          rom.load(romNbt)
        })
        tmp.foreach(tmp => {
          val tmpNbt = computerNbt.getCompoundTag("tmp")
          tmp.node.load(tmpNbt)
          tmp.load(tmpNbt)
        })
        kernelMemory = computerNbt.getInteger("kernelMemory")
        timeStarted = computerNbt.getLong("timeStarted")
        cpuTime = computerNbt.getLong("cpuTime")
        if (computerNbt.hasKey("message")) {
          message = Some(computerNbt.getString("message"))
        }

        // Limit memory again.
        recomputeMemory()

        // Ensure the executor is started in the next update if necessary.
        assert(future.isEmpty)
        state match {
          case Computer.State.Yielded =>
            state = Computer.State.Paused
          case Computer.State.SynchronizedReturn =>
            state = Computer.State.SynchronizedReturnPaused
          case _ => // Will be started by update() if necessary.
        }

        // Delay execution for a second to allow the world around us to settle.
        sleepUntil = System.currentTimeMillis() + 1000
      } catch {
        case e: LuaRuntimeException => {
          OpenComputers.log.warning("Could not unpersist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
          close()
        }
      }
    }
    // Init failed, or we were already stopped.
    else state = Computer.State.Stopped
  }

  def save(nbt: NBTTagCompound): Unit = this.synchronized {
    assert(state != Computer.State.Running) // Lock on 'this' should guarantee this.
    assert(state != Computer.State.Stopping) // Only set while executor is running.

    processAddedComponents()

    val computerNbt = new NBTTagCompound()

    computerNbt.setInteger("state", (state match {
      case Computer.State.Paused => Computer.State.Yielded
      case Computer.State.SynchronizedReturnPaused => Computer.State.SynchronizedReturn
      case Computer.State.Sleeping => Computer.State.Yielded
      case other => other
    }).id)
    if (state != Computer.State.Stopped) {
      // Unlimit memory while persisting.
      lua.setTotalMemory(Integer.MAX_VALUE)

      try {
        // Try persisting Lua, because that's what all of the rest depends on.
        // Save the kernel state (which is always at stack index one).
        assert(lua.isThread(1))
        computerNbt.setByteArray("kernel", persist(1))
        // While in a driver call we have one object on the global stack: either
        // the function to call the driver with, or the result of the call.
        if (state == Computer.State.SynchronizedCall || state == Computer.State.SynchronizedReturn || state == Computer.State.SynchronizedReturnPaused) {
          assert(if (state == Computer.State.SynchronizedCall) lua.isFunction(2) else lua.isTable(2))
          computerNbt.setByteArray("stack", persist(2))
        }

        val componentsNbt = new NBTTagList()
        for ((address, name) <- components) {
          val componentNbt = new NBTTagCompound()
          componentNbt.setString("address", address)
          componentNbt.setString("name", name)
          componentsNbt.appendTag(componentNbt)
        }
        computerNbt.setTag("components", componentsNbt)

        val signalsNbt = new NBTTagList()
        for (s <- signals.iterator) {
          val signalNbt = new NBTTagCompound()
          signalNbt.setString("name", s.name)
          val args = new NBTTagCompound()
          args.setInteger("length", s.args.length)
          s.args.zipWithIndex.foreach {
            case (Unit, i) => args.setByte("arg" + i, -1)
            case (arg: Boolean, i) => args.setByte("arg" + i, if (arg) 1 else 0)
            case (arg: Double, i) => args.setDouble("arg" + i, arg)
            case (arg: String, i) => args.setString("arg" + i, arg)
            case (arg: Array[Byte], i) => args.setByteArray("arg" + i, arg)
          }
          signalNbt.setCompoundTag("args", args)
          signalsNbt.appendTag(signalNbt)
        }
        computerNbt.setTag("signals", signalsNbt)

        val romNbt = new NBTTagCompound()
        rom.foreach(rom => {
          rom.save(romNbt)
          rom.node.save(romNbt)
        })
        computerNbt.setCompoundTag("rom", romNbt)

        val tmpNbt = new NBTTagCompound()
        tmp.foreach(tmp => {
          tmp.save(tmpNbt)
          tmp.node.save(tmpNbt)
        })
        computerNbt.setCompoundTag("tmp", tmpNbt)

        computerNbt.setInteger("kernelMemory", kernelMemory)
        computerNbt.setLong("timeStarted", timeStarted)
        computerNbt.setLong("cpuTime", cpuTime)
        message.foreach(computerNbt.setString("message", _))
      } catch {
        case e: LuaRuntimeException => {
          OpenComputers.log.warning("Could not persist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
          computerNbt.setInteger("state", Computer.State.Stopped.id)
        }
      }

      // Limit memory again.
      recomputeMemory()
    }

    nbt.setCompoundTag("oc.computer", computerNbt)
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

  // ----------------------------------------------------------------------- //

  private def init(): Boolean = {
    // Utility functions for varargs callbacks.
    def parseArgument(lua: LuaState, index: Int): AnyRef = lua.`type`(index) match {
      case LuaType.BOOLEAN => Boolean.box(lua.toBoolean(index))
      case LuaType.NUMBER => Double.box(lua.toNumber(index))
      case LuaType.STRING => lua.toByteArray(index)
      case _ => Unit
    }

    def parseArguments(lua: LuaState, start: Int) =
      for (index <- start to lua.getTop) yield parseArgument(lua, index)

    def pushList(value: Iterator[(Any, Int)]) {
      lua.newTable()
      var count = 0
      value.foreach {
        case (x, index) => {
          x match {
            case (entry: ScalaNumber) =>
              pushResult(lua, entry.underlying())
            case (entry) =>
              pushResult(lua, entry.asInstanceOf[AnyRef])
          }
          lua.rawSet(-2, index + 1)
          count = count + 1
        }
      }
      lua.pushString("n")
      lua.pushInteger(count)
      lua.rawSet(-3)
    }

    def pushResult(lua: LuaState, value: AnyRef): Unit = value match {
      case null | Unit => lua.pushNil()
      case value: java.lang.Boolean => lua.pushBoolean(value.booleanValue)
      case value: java.lang.Byte => lua.pushNumber(value.byteValue)
      case value: java.lang.Character => lua.pushString(String.valueOf(value))
      case value: java.lang.Short => lua.pushNumber(value.shortValue)
      case value: java.lang.Integer => lua.pushNumber(value.intValue)
      case value: java.lang.Long => lua.pushNumber(value.longValue)
      case value: java.lang.Float => lua.pushNumber(value.floatValue)
      case value: java.lang.Double => lua.pushNumber(value.doubleValue)
      case value: java.lang.String => lua.pushString(value)
      case value: Array[Byte] => lua.pushByteArray(value)
      case value: Array[_] => pushList(value.zipWithIndex.iterator)
      case value: Product => pushList(value.productIterator.zipWithIndex)
      case value: Seq[_] => pushList(value.zipWithIndex.iterator)
      // TODO maps?
      case _ =>
        OpenComputers.log.warning("A component callback tried to return an unsupported value of type " + value.getClass.getName + ".")
        lua.pushNil()
    }

    // Reset error state.
    message = None

    // Creates a new state with all base libraries and the persistence library
    // loaded into it. This means the state has much more power than it
    // rightfully should have, so we sandbox it a bit in the following.
    LuaStateFactory.createState() match {
      case None =>
        lua = null
        return false
      case Some(value) => lua = value
    }

    // Connect the ROM and `/tmp` node to our owner. We're not in a network in
    // case we're loading, which is why we have to check it here.
    if (owner.node.network != null) {
      rom.foreach(rom => owner.node.connect(rom.node))
      tmp.foreach(tmp => owner.node.connect(tmp.node))
    }

    try {
      // Push a couple of functions that override original Lua API functions or
      // that add new functionality to it.

      // Push a couple of functions that override original Lua API functions or
      // that add new functionality to it.
      lua.getGlobal("os")

      // Custom os.clock() implementation returning the time the computer has
      // been actively running, instead of the native library...
      lua.pushScalaFunction(lua => {
        lua.pushNumber((cpuTime + (System.nanoTime() - cpuStart)) * 10e-10)
        1
      })
      lua.setField(-2, "clock")

      // Return ingame time for os.time().
      lua.pushScalaFunction(lua => {
        // Game time is in ticks, so that each day has 24000 ticks, meaning
        // one hour is game time divided by one thousand. Also, Minecraft
        // starts days at 6 o'clock, so we add those six hours. Thus:
        // timestamp = (time + 6000) / 1000[h] * 60[m] * 60[s] * 1000[ms]
        lua.pushNumber((worldTime + 6000) * 60 * 60)
        1
      })
      lua.setField(-2, "time")

      // The time the computer has been running, as opposed to the CPU time.
      lua.pushScalaFunction(lua => {
        // World time is in ticks, and each second has 20 ticks. Since we
        // want os.uptime() to return real seconds, though, we'll divide it
        // accordingly.
        lua.pushNumber((worldTime - timeStarted) / 20.0)
        1
      })
      lua.setField(-2, "uptime")

      // Allow the system to read how much memory it uses and has available.
      lua.pushScalaFunction(lua => {
        lua.pushInteger(lua.getTotalMemory - kernelMemory)
        1
      })
      lua.setField(-2, "totalMemory")

      lua.pushScalaFunction(lua => {
        // This is *very* unlikely, but still: avoid this getting larger than
        // what we report as the total memory.
        lua.pushInteger(lua.getFreeMemory min (lua.getTotalMemory - kernelMemory))
        1
      })
      lua.setField(-2, "freeMemory")

      lua.pushScalaFunction(lua => {
        lua.pushBoolean(signal(lua.checkString(1), parseArguments(lua, 2): _*))
        1
      })
      lua.setField(-2, "pushSignal")

      // Allow the computer to figure out its own id in the component network.
      lua.pushScalaFunction(lua => {
        Option(owner.node.address) match {
          case None => lua.pushNil()
          case Some(address) => lua.pushString(address)
        }
        1
      })
      lua.setField(-2, "address")

      // And it's ROM address.
      lua.pushScalaFunction(lua => {
        rom.foreach(rom => Option(rom.node.address) match {
          case None => lua.pushNil()
          case Some(address) => lua.pushString(address)
        })
        1
      })
      lua.setField(-2, "romAddress")

      // And it's /tmp address...
      lua.pushScalaFunction(lua => {
        tmp.foreach(tmp => Option(tmp.node.address) match {
          case None => lua.pushNil()
          case Some(address) => lua.pushString(address)
        })
        1
      })
      lua.setField(-2, "tmpAddress")

      // Pop the os table.
      lua.pop(1)

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

      // How long programs may run without yielding before we stop them.
      lua.pushNumber(Config.timeout)
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
          case Some(name: String) =>
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
        Option(owner.node.network.node(lua.checkString(1))) match {
          case Some(component: server.network.Component) if component.canBeSeenFrom(owner.node) =>
            lua.newTable()
            for (method <- component.methods()) {
              lua.pushString(method)
              lua.pushBoolean(component.isAsynchronous(method))
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
        val args = parseArguments(lua, 3)
        try {
          (Option(owner.node.network.node(address)) match {
            case Some(component: Component) if component.canBeSeenFrom(owner.node) =>
              component.invoke(method, owner, args: _*)
            case _ => throw new Exception("no such component")
          }) match {
            case results: Array[_] =>
              lua.pushBoolean(true)
              results.foreach(pushResult(lua, _))
              1 + results.length
            case _ =>
              lua.pushBoolean(true)
              1
          }
        } catch {
          case e: IllegalArgumentException if e.getMessage != null =>
            lua.pushBoolean(false)
            lua.pushString(e.getMessage)
            2
          case e: Throwable if e.getMessage != null =>
            lua.pushBoolean(true)
            lua.pushNil()
            lua.pushString(e.getMessage)
            3
          case _: ArrayIndexOutOfBoundsException =>
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
          case _: Throwable =>
            lua.pushBoolean(true)
            lua.pushNil()
            lua.pushString("unknown error")
            3
        }
      })
      lua.setField(-2, "invoke")

      lua.setGlobal("component")

      // Run the boot script. This sets up the permanent value tables as
      // well as making the functions used for persisting/unpersisting
      // available as globals. It also wraps the message sending functions
      // so that they yield a closure doing the actual call so that that
      // message call can be performed in a synchronized fashion.
      //      lua.load(classOf[Computer].getResourceAsStream(Config.scriptPath + "boot.lua"), "=boot", "t")
      //      lua.call(0, 0)
      initPerms()

      // Load the basic kernel which sets up the sandbox, loads the init script
      // and then runs it in a coroutine with a debug hook checking for
      // timeouts.
      lua.load(classOf[Computer].getResourceAsStream(Config.scriptPath + "kernel.lua"), "=kernel", "t")
      lua.newThread() // Left as the first value on the stack.

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

  private def close() = stateMonitor.synchronized(
    if (state != Computer.State.Stopped) {
      state = Computer.State.Stopped
      lua.setTotalMemory(Integer.MAX_VALUE)
      lua.close()
      lua = null
      kernelMemory = 0
      signals.clear()
      timeStarted = 0
      cpuTime = 0
      cpuStart = 0
      future = None
      sleepUntil = 0

      // Mark state change in owner, to send it to clients.
      owner.markAsChanged(Double.NegativeInfinity)
    })

  // ----------------------------------------------------------------------- //

  private def execute(value: Computer.State.Value) {
    assert(future.isEmpty)
    sleepUntil = 0
    state = value
    future = Some(Computer.Executor.pool.submit(this))
  }

  // This is a really high level lock that we only use for saving and loading.
  override def run(): Unit = this.synchronized {
    val callReturn = stateMonitor.synchronized {
      val oldState = state
      state = Computer.State.Running

      // See if the game appears to be paused, in which case we also pause.
      if (System.currentTimeMillis - lastUpdate > 50) {
        state = oldState match {
          case Computer.State.SynchronizedReturn => Computer.State.SynchronizedReturnPaused
          case _ => Computer.State.Paused
        }
        future = None
        return
      }

      oldState
    } match {
      case Computer.State.SynchronizedReturn => true
      case Computer.State.Yielded | Computer.State.Sleeping => false
      case Computer.State.Stopping =>
        // stop() was called directly after start(), e.g. due to lack of power.
        close()
        return
      case s =>
        OpenComputers.log.warning("Running computer from invalid state " + s.toString + ". This is a bug!")
        close()
        return
    }

    // The kernel thread will always be at stack index one.
    assert(lua.isThread(1))

    try {
      // Help out the GC a little. The emergency GC has a few limitations that
      // will make it free less memory than doing a full step manually.
      lua.gc(LuaState.GcAction.COLLECT, 0)
      // Resume the Lua state and remember the number of results we get.
      cpuStart = System.nanoTime()
      val results = if (callReturn) {
        // If we were doing a synchronized call, continue where we left off.
        assert(lua.getTop == 2)
        assert(lua.isTable(2))
        lua.resume(1, 1)
      }
      else stateMonitor.synchronized(signals.dequeueFirst(_ => true)) match {
        case Some(signal) =>
          lua.pushString(signal.name)
          signal.args.foreach {
            case Unit => lua.pushNil()
            case arg: Boolean => lua.pushBoolean(arg)
            case arg: Double => lua.pushNumber(arg)
            case arg: String => lua.pushString(arg)
            case arg: Array[Byte] => lua.pushByteArray(arg)
          }
          lua.resume(1, 1 + signal.args.length)
        case _ =>
          lua.resume(1, 0)
      }

      // Check if this was the first run, meaning the one used for initializing
      // the kernel (loading the libs, establishing a memory baseline).
      val runtime = if (kernelMemory == 0) {
        // Run the garbage collector to get rid of stuff left behind after the
        // initialization phase to get a good estimate of the base memory usage
        // the kernel has. We remember that size to grant user-space programs a
        // fixed base amount of memory, regardless of the memory need of the
        // underlying system (which may change across releases).
        lua.gc(LuaState.GcAction.COLLECT, 0)
        kernelMemory = (lua.getTotalMemory - lua.getFreeMemory) + Config.baseMemory
        recomputeMemory()
        0
      }
      else {
        System.nanoTime() - cpuStart
      }
      cpuTime += runtime

      // Check if the kernel is still alive.
      stateMonitor.synchronized(if (lua.status(1) == LuaState.YIELD) {
        // Intermediate state in some cases. Satisfies the assert in execute().
        future = None
        // Someone called stop() in the meantime.
        if (state == Computer.State.Stopping)
          close()
        // If we have a single number that's how long we may wait before
        // resuming the state again.
        else if (results == 1 && lua.isNumber(2)) {
          val sleep = lua.toNumber(2) * 1000
          lua.pop(results)
          // But only sleep if we don't have more signals to process.
          if (signals.isEmpty) {
            state = Computer.State.Sleeping
            sleepUntil = System.currentTimeMillis + sleep
          }
          else execute(Computer.State.Yielded)
        }
        // If we get one function it must be a wrapper for a synchronized call.
        // The protocol is that a closure is pushed that is then called from
        // the main server thread, and returns a table, which is in turn passed
        // to the originating coroutine.yield().
        else if (results == 1 && lua.isFunction(2))
          state = Computer.State.SynchronizedCall
        // Check if we are shutting down, and if so if we're rebooting. This is
        // signalled by boolean values, where `false` means shut down, `true`
        // means reboot (i.e shutdown then start again).
        else if (results == 1 && lua.isBoolean(2)) {
          if (lua.toBoolean(2))
            state = Computer.State.Rebooting
          else
            close()
        }
        else {
          // Something else, just pop the results and try again.
          lua.pop(results)
          if (signals.isEmpty)
            state = Computer.State.Suspended
          else
            execute(Computer.State.Yielded)
        }

        // State has inevitably changed, mark as changed to save again.
        owner.markAsChanged(Config.computerCpuTimeCost * runtime / 1000 / 1000 / 1000)
      }
      // The kernel thread returned. If it threw we'd we in the catch below.
      else {
        assert(lua.isThread(1))
        // We're expecting the result of a pcall, if anything, so boolean + (result | string).
        if (!lua.isBoolean(2) || !(lua.isString(3) || lua.isNil(3))) {
          OpenComputers.log.warning("Kernel returned unexpected results.")
        }
        // The pcall *should* never return normally... but check for it nonetheless.
        if (lua.toBoolean(2)) {
          OpenComputers.log.warning("Kernel stopped unexpectedly.")
        }
        else {
          lua.setTotalMemory(Int.MaxValue)
          val error = lua.toString(3)
          if (error != null)
            message = Some(error)
          else
            message = Some("unknown error")
        }
        close()
      })
    }
    catch {
      case e: LuaRuntimeException =>
        OpenComputers.log.warning("Kernel crashed. This is a bug!\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        message = Some("kernel panic")
        close()
      case e: LuaGcMetamethodException =>
        if (e.getMessage != null)
          message = Some("kernel panic:\n" + e.getMessage)
        else
          message = Some("kernel panic:\nerror in garbage collection metamethod")
        close()
      case e: LuaMemoryAllocationException =>
        message = Some("not enough memory")
        close()
      case e: java.lang.Error if e.getMessage == "not enough memory" =>
        message = Some("not enough memory")
        close()
      case e: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Unexpected error in kernel. This is a bug!\n", e)
        message = Some("kernel panic")
        close()
    }
  }
}

object Computer {

  trait Environment extends tileentity.Environment with oc.util.Persistable with Context {
    val node = api.Network.newNode(this, Visibility.Network).
      withComponent("computer", Visibility.Neighbors).
      withConnector(Config.bufferComputer).
      create()

    val instance: Computer

    def world: World

    def installedMemory: Int

    /**
     * Called when the computer state changed, so it should be saved again.
     * <p/>
     * This is called asynchronously from the Computer's executor thread, so the
     * computer's owner must make sure to handle this in a synchronized fashion.
     *
     * @param power the power that should be consumed.
     */
    def markAsChanged(power: Double): Unit

    // ----------------------------------------------------------------------- //

    def address = node.address

    def signal(name: String, args: AnyRef*) = instance.signal(name, args: _*)

    // ----------------------------------------------------------------------- //

    @LuaCallback("start")
    def start(context: Context, args: Arguments): Array[AnyRef] =
      Array(Boolean.box(instance.start()))

    @LuaCallback("stop")
    def stop(context: Context, args: Arguments): Array[AnyRef] =
      Array(Boolean.box(instance.stop()))

    @LuaCallback("isRunning")
    def isRunning(context: Context, args: Arguments): Array[AnyRef] =
      Array(Boolean.box(instance.isRunning))

    // ----------------------------------------------------------------------- //

    override def onMessage(message: Message) = {
      super.onMessage(message)
      message.data match {
        case Array(name: String, args@_*) if message.name == "computer.signal" =>
          instance.signal(name, Seq(message.source.address) ++ args: _*)
        case _ =>
      }
    }

    override def onConnect(node: Node) {
      super.onConnect(node)
      if (node == this.node) {
        instance.rom.foreach(rom => node.connect(rom.node))
        instance.tmp.foreach(tmp => node.connect(tmp.node))
      }
      else {
        node match {
          case component: Component => instance.addComponent(component)
          case _ =>
        }
      }
    }

    override def onDisconnect(node: Node) {
      super.onDisconnect(node)
      if (node == this.node) {
        instance.rom.foreach(_.node.remove())
        instance.tmp.foreach(_.node.remove())
      }
      else {
        node match {
          case component: Component => instance.removeComponent(component)
          case _ =>
        }
      }
    }

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      if (node != null) node.load(nbt)
      if (instance != null) instance.load(nbt)
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      if (node != null) node.save(nbt)
      if (instance != null) instance.save(nbt)
    }
  }

  @ForgeSubscribe
  def onChunkUnload(e: ChunkEvent.Unload) =
    onUnload(e.world, e.getChunk.chunkTileEntityMap.values.map(_.asInstanceOf[TileEntity]))

  private def onUnload(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    tileEntities.
      filter(_.isInstanceOf[tileentity.Computer]).
      map(_.asInstanceOf[tileentity.Computer]).
      foreach(_.turnOff())
  }

  /** Signals are messages sent to the Lua state from Java asynchronously. */
  private class Signal(val name: String, val args: Array[Any])

  /** Possible states of the computer, and in particular its executor. */
  private object State extends Enumeration {
    /** The computer is not running right now and there is no Lua state. */
    val Stopped = Value("Stopped")

    /** The computer is running but yielded and there were no more signals to process. */
    val Suspended = Value("Suspended")

    /** The computer is running but yielded but will resume as soon as possible. */
    val Yielded = Value("Yielded")

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

    /** Computer is currently rebooting. */
    val Rebooting = Value("Rebooting")
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
          val name = OpenComputers.getClass.getSimpleName + "-" + threadNumber.getAndIncrement
          val thread = new Thread(group, r, name)
          if (!thread.isDaemon)
            thread.setDaemon(true)
          if (thread.getPriority != Thread.MIN_PRIORITY)
            thread.setPriority(Thread.MIN_PRIORITY)
          thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
            def uncaughtException(t: Thread, e: Throwable) {
              OpenComputers.log.log(Level.WARNING, "Unhandled exception in worker thread.", e)
            }
          })
          thread
        }
      })
  }

}