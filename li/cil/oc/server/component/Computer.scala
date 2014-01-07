package li.cil.oc.server.component

import com.naef.jnlua._
import java.io.{FileNotFoundException, IOException}
import java.util.logging.Level
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity
import li.cil.oc.server
import li.cil.oc.server.PacketSender
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.{ThreadPoolFactory, GameTimeFormatter, LuaStateFactory}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt._
import net.minecraft.server.MinecraftServer
import net.minecraft.server.integrated.IntegratedServer
import scala.Array.canBuildFrom
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Computer(val owner: tileentity.Computer) extends ManagedComponent with Context with Runnable {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("computer", Visibility.Neighbors).
    withConnector(if (isRobot) Settings.get.bufferRobot + 30 * Settings.get.bufferPerLevel else Settings.get.bufferComputer).
    create()

  val rom = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/rom"), "rom"))

  val tmp = if (Settings.get.tmpSize > 0) {
    Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
      fromMemory(Settings.get.tmpSize * 1024), "tmpfs"))
  } else None

  private val state = mutable.Stack(Computer.State.Stopped)

  private var lua: LuaState = null

  private var kernelMemory = 0

  private val components = mutable.Map.empty[String, String]

  private val addedComponents = mutable.Set.empty[Component]

  private val _users = mutable.Set.empty[String]

  private val signals = new mutable.Queue[Computer.Signal]

  private val callCounts = mutable.Map.empty[String, mutable.Map[String, Int]]

  private val ramScale = if (LuaStateFactory.is64Bit) Settings.get.ramScaleFor64Bit else 1.0

  // ----------------------------------------------------------------------- //

  private var timeStarted = 0L // Game-world time [ms] for os.uptime().

  private var worldTime = 0L // Game-world time for os.time().

  private var cpuTime = 0L // Pseudo-real-world time [ns] for os.clock().

  private var cpuStart = 0L // Pseudo-real-world time [ns] for os.clock().

  private var remainIdle = 0 // Ticks left to sleep before resuming.

  private var remainingPause = 0 // Ticks left to wait before resuming.

  private var usersChanged = false // Send updated users list to clients?

  private var message: Option[String] = None // For error messages.

  // ----------------------------------------------------------------------- //

  def recomputeMemory() = Option(lua) match {
    case Some(l) =>
      l.setTotalMemory(Int.MaxValue)
      l.gc(LuaState.GcAction.COLLECT, 0)
      if (kernelMemory > 0) {
        l.setTotalMemory(kernelMemory + math.ceil(owner.installedMemory * ramScale).toInt)
      }
    case _ =>
  }

  def lastError = message

  def users = _users.synchronized(_users.toArray)

  def isRobot = false

  private val cost = (if (isRobot) Settings.get.robotCost else Settings.get.computerCost) * Settings.get.tickFrequency

  // ----------------------------------------------------------------------- //

  def address = node.address

  def canInteract(player: String) = !Settings.get.canComputersBeOwned ||
    _users.synchronized(_users.isEmpty || _users.contains(player)) ||
    MinecraftServer.getServer.isSinglePlayer ||
    MinecraftServer.getServer.getConfigurationManager.isPlayerOpped(player)

  def isRunning = state.synchronized(state.top != Computer.State.Stopped && state.top != Computer.State.Stopping)

  def isPaused = state.synchronized(state.top == Computer.State.Paused && remainingPause > 0)

  def start() = state.synchronized(state.top match {
    case Computer.State.Stopped =>
      val rules = owner.world.getWorldInfo.getGameRulesInstance
      if (rules.hasRule("doDaylightCycle") && !rules.getGameRuleBooleanValue("doDaylightCycle")) {
        crash("computers don't work while time is frozen (gamerule doDaylightCycle is false)")
        false
      }
      else if (owner.installedMemory > 0) {
        if (Settings.get.ignorePower || node.globalBuffer > cost) {
          init() && {
            switchTo(Computer.State.Starting)
            timeStarted = owner.world.getWorldTime
            node.sendToReachable("computer.started")
            true
          }
        }
        else {
          message = Some("not enough energy")
          false
        }
      }
      else {
        message = Some("no memory installed")
        false
      }
    case Computer.State.Paused if remainingPause > 0 =>
      remainingPause = 0
      true
    case Computer.State.Stopping =>
      switchTo(Computer.State.Restarting)
      true
    case _ =>
      false
  })

  def pause(seconds: Double): Boolean = {
    val ticksToPause = math.max((seconds * 20).toInt, 0)
    def shouldPause(state: Computer.State.Value) = state match {
      case Computer.State.Stopping | Computer.State.Stopped => false
      case Computer.State.Paused if ticksToPause <= remainingPause => false
      case _ => true
    }
    if (shouldPause(state.synchronized(state.top))) {
      // Check again when we get the lock, might have changed since.
      this.synchronized(state.synchronized(if (shouldPause(state.top)) {
        if (state.top != Computer.State.Paused) {
          assert(!state.contains(Computer.State.Paused))
          state.push(Computer.State.Paused)
        }
        remainingPause = ticksToPause
        owner.markAsChanged()
        return true
      }))
    }
    false
  }

  def stop() = state.synchronized(state.top match {
    case Computer.State.Stopped | Computer.State.Stopping =>
      false
    case _ =>
      state.push(Computer.State.Stopping)
      true
  })

  protected def crash(message: String) = {
    this.message = Option(message)
    stop()
  }

  def signal(name: String, args: AnyRef*) = state.synchronized(state.top match {
    case Computer.State.Stopped | Computer.State.Stopping => false
    case _ => signals.synchronized {
      if (signals.size >= 256) false
      else {
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
          case arg: Map[String, String] => arg
          case arg =>
            OpenComputers.log.warning("Trying to push signal with an unsupported argument of type " + arg.getClass.getName)
            Unit
        }.toArray))
        true
      }
    }
  })

  // ----------------------------------------------------------------------- //

  @LuaCallback("start")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!isPaused && start())

  @LuaCallback("stop")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(stop())

  @LuaCallback(value = "isRunning", direct = true)
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(isRunning)

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() = if (state.synchronized(state.top != Computer.State.Stopped)) {
    // Add components that were added since the last update to the actual list
    // of components if we can see them. We use this delayed approach to avoid
    // issues with components that have a visibility lower than their
    // reachability, because in that case if they get connected in the wrong
    // order we wouldn't add them (since they'd be invisible in their connect
    // message, and only become visible with a later node-to-node connection,
    // but that wouldn't trigger a connect message anymore due to the higher
    // reachability).
    processAddedComponents()

    // Update world time for time().
    worldTime = owner.world.getWorldTime

    // We can have rollbacks from '/time set'. Avoid getting negative uptimes.
    timeStarted = math.min(timeStarted, worldTime)

    if (remainIdle > 0) {
      remainIdle -= 1
    }

    // Reset direct call limits.
    callCounts.synchronized(if (callCounts.size > 0) callCounts.clear())

    // Make sure we have enough power.
    if (worldTime % Settings.get.tickFrequency == 0) {
      state.synchronized(state.top match {
        case Computer.State.Paused |
             Computer.State.Restarting |
             Computer.State.Stopping |
             Computer.State.Stopped => // No power consumption.
        case Computer.State.Sleeping if remainIdle > 0 && signals.isEmpty =>
          if (!node.tryChangeBuffer(-cost * Settings.get.sleepCostFactor)) {
            crash("not enough energy")
          }
        case _ =>
          if (!node.tryChangeBuffer(-cost)) {
            crash("not enough energy")
          }
      })
    }

    // Avoid spamming user list across the network.
    if (worldTime % 20 == 0 && usersChanged) {
      val list = _users.synchronized {
        usersChanged = false
        users
      }
      PacketSender.sendComputerUserList(owner, list)
    }

    // Check if we should switch states. These are all the states in which we're
    // guaranteed that the executor thread isn't running anymore.
    state.synchronized(state.top match {
      // Booting up.
      case Computer.State.Starting =>
        verifyComponents()
        switchTo(Computer.State.Yielded)
      // Computer is rebooting.
      case Computer.State.Restarting =>
        close()
        tmp.foreach(_.node.remove()) // To force deleting contents.
        node.sendToReachable("computer.stopped")
        start()
      // Resume from pauses based on sleep or signal underflow.
      case Computer.State.Sleeping if remainIdle <= 0 || !signals.isEmpty =>
        switchTo(Computer.State.Yielded)
      // Resume in case we paused  because the game was paused.
      case Computer.State.Paused =>
        if (remainingPause > 0) {
          remainingPause -= 1
        }
        else {
          verifyComponents() // In case we're resuming after loading.
          state.pop()
          switchTo(state.top) // Trigger execution if necessary.
        }
      // Perform a synchronized call (message sending).
      case Computer.State.SynchronizedCall =>
        // These three asserts are all guaranteed by run().
        assert(lua.getTop == 2)
        assert(lua.isThread(1))
        assert(lua.isFunction(2))
        // Clear direct call limits again, just to be on the safe side...
        // Theoretically it'd be possible for the executor to do some direct
        // calls between the clear and the state check, which could in turn
        // make this synchronized call fail due the limit still being maxed.
        callCounts.clear()
        // We switch into running state, since we'll behave as though the call
        // were performed from our executor thread.
        switchTo(Computer.State.Running)
        try {
          // Synchronized call protocol requires the called function to return
          // a table, which holds the results of the call, to be passed back
          // to the coroutine.yield() that triggered the call.
          lua.call(0, 1)
          lua.checkType(2, LuaType.TABLE)
          // Check if the callback called pause() or stop().
          state.top match {
            case Computer.State.Running =>
              switchTo(Computer.State.SynchronizedReturn)
            case Computer.State.Paused =>
              state.pop() // Paused
              state.pop() // Running, no switchTo to avoid new future.
              state.push(Computer.State.SynchronizedReturn)
              state.push(Computer.State.Paused)
            case Computer.State.Stopping => // Nothing to do, we'll die anyway.
            case _ => throw new AssertionError()
          }
        } catch {
          case _: LuaMemoryAllocationException =>
            // This can happen if we run out of memory while converting a Java
            // exception to a string (which we have to do to avoid keeping
            // userdata on the stack, which cannot be persisted).
            crash("not enough memory")
          case e: java.lang.Error if e.getMessage == "not enough memory" =>
            crash("not enough memory")
          case e: Throwable =>
            OpenComputers.log.log(Level.WARNING, "Faulty Lua implementation for synchronized calls.", e)
            crash("protocol error")
        }
      case _ => // Nothing special to do, just avoid match errors.
    })

    // Finally check if we should stop the computer. We cannot lock the state
    // because we may have to wait for the executor thread to finish, which
    // might turn into a deadlock depending on where it currently is.
    state.synchronized(state.top) match {
      // Computer is shutting down.
      case Computer.State.Stopping => this.synchronized(state.synchronized {
        close()
        rom.foreach(_.node.remove())
        tmp.foreach(_.node.remove())
        node.sendToReachable("computer.stopped")
      })
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    message.data match {
      case Array(name: String, args@_*) if message.name == "computer.signal" =>
        signal(name, Seq(message.source.address) ++ args: _*)
      case Array(player: EntityPlayer, name: String, args@_*) if message.name == "computer.checked_signal" =>
        if (canInteract(player.getCommandSenderName))
          signal(name, Seq(message.source.address) ++ args: _*)
      case _ =>
    }
  }

  override def onConnect(node: Node) {
    if (node == this.node) {
      components += this.node.address -> this.node.name
      rom.foreach(rom => node.connect(rom.node))
      tmp.foreach(tmp => node.connect(tmp.node))
    }
    else {
      node match {
        case component: Component => addComponent(component)
        case _ =>
      }
    }
    // For computers, to generate the components in their inventory.
    owner.onConnect(node)
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      stop()
      rom.foreach(_.node.remove())
      tmp.foreach(_.node.remove())
    }
    else {
      node match {
        case component: Component => removeComponent(component)
        case _ =>
      }
    }
    // For computers, to save the components in their inventory.
    owner.onDisconnect(node)
  }

  // ----------------------------------------------------------------------- //

  def addComponent(component: Component) {
    if (!components.contains(component.address)) {
      addedComponents += component
    }
  }

  def removeComponent(component: Component) {
    if (components.contains(component.address)) {
      components.synchronized(components -= component.address)
      signal("component_removed", component.address, component.name)
    }
    addedComponents -= component
  }

  private def processAddedComponents() {
    if (addedComponents.size > 0) {
      for (component <- addedComponents) {
        if (component.canBeSeenFrom(node)) {
          components.synchronized(components += component.address -> component.name)
          // Skip the signal if we're not initialized yet, since we'd generate a
          // duplicate in the startup script otherwise.
          if (kernelMemory > 0) {
            signal("component_added", component.address, component.name)
          }
        }
      }
      addedComponents.clear()
    }
  }

  private def verifyComponents() {
    val invalid = mutable.Set.empty[String]
    for ((address, name) <- components) {
      if (node.network.node(address) == null) {
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

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = this.synchronized {
    assert(state.top == Computer.State.Stopped)
    assert(_users.isEmpty)
    assert(signals.isEmpty)
    state.clear()

    super.load(nbt)

    state.pushAll(nbt.getTagList("state").iterator[NBTTagInt].reverse.map(s => Computer.State(s.data)))
    nbt.getTagList("users").foreach[NBTTagString](u => _users += u.data)

    if (state.size > 0 && state.top != Computer.State.Stopped && init()) {
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
        if (state.contains(Computer.State.SynchronizedCall) || state.contains(Computer.State.SynchronizedReturn)) {
          unpersist(nbt.getByteArray("stack"))
          if (!(if (state.contains(Computer.State.SynchronizedCall)) lua.isFunction(2) else lua.isTable(2))) {
            // Same as with the above, should not really happen normally, but
            // could for the same reasons.
            throw new IllegalArgumentException("Invalid stack.")
          }
        }

        components ++= nbt.getTagList("components").iterator[NBTTagCompound].map(c =>
          c.getString("address") -> c.getString("name"))

        signals ++= nbt.getTagList("signals").iterator[NBTTagCompound].map(signalNbt => {
          val argsNbt = signalNbt.getCompoundTag("args")
          val argsLength = argsNbt.getInteger("length")
          new Computer.Signal(signalNbt.getString("name"),
            (0 until argsLength).map("arg" + _).map(argsNbt.getTag).map {
              case tag: NBTTagByte if tag.data == -1 => Unit
              case tag: NBTTagByte => tag.data == 1
              case tag: NBTTagDouble => tag.data
              case tag: NBTTagString => tag.data
              case tag: NBTTagByteArray => tag.byteArray
              case tag: NBTTagList =>
                val data = mutable.Map.empty[String, String]
                for (i <- 0 until tag.tagCount by 2) {
                  (tag.tagAt(i), tag.tagAt(i + 1)) match {
                    case (key: NBTTagString, value: NBTTagString) => data += key.data -> value.data
                    case _ =>
                  }
                }
                data
              case _ => Unit
            }.toArray)
        })

        rom.foreach(rom => rom.load(nbt.getCompoundTag("rom")))
        tmp.foreach(tmp => tmp.load(nbt.getCompoundTag("tmp")))
        kernelMemory = (nbt.getInteger("kernelMemory") * ramScale).toInt
        timeStarted = nbt.getLong("timeStarted")
        cpuTime = nbt.getLong("cpuTime")
        remainingPause = nbt.getInteger("remainingPause")
        if (nbt.hasKey("message")) {
          message = Some(nbt.getString("message"))
        }

        // Limit memory again.
        recomputeMemory()

        // Delay execution for a second to allow the world around us to settle.
        pause(Settings.get.startupDelay)
      } catch {
        case e: LuaRuntimeException =>
          OpenComputers.log.warning("Could not unpersist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
          state.push(Computer.State.Stopping)
      }
    }
    else close() // Clean up in case we got a weird state stack.
  }

  override def save(nbt: NBTTagCompound): Unit = this.synchronized {
    assert(state.top != Computer.State.Running) // Lock on 'this' should guarantee this.

    // Make sure we don't continue running until everything has saved.
    pause(0.05)

    super.save(nbt)

    // Make sure the component list is up-to-date.
    processAddedComponents()

    nbt.setNewTagList("state", state.map(_.id))
    nbt.setNewTagList("users", _users)

    if (state.top != Computer.State.Stopped) {
      // Unlimit memory while persisting.
      lua.setTotalMemory(Integer.MAX_VALUE)

      try {
        // Try persisting Lua, because that's what all of the rest depends on.
        // Save the kernel state (which is always at stack index one).
        assert(lua.isThread(1))
        nbt.setByteArray("kernel", persist(1))
        // While in a driver call we have one object on the global stack: either
        // the function to call the driver with, or the result of the call.
        if (state.contains(Computer.State.SynchronizedCall) || state.contains(Computer.State.SynchronizedReturn)) {
          assert(if (state.contains(Computer.State.SynchronizedCall)) lua.isFunction(2) else lua.isTable(2))
          nbt.setByteArray("stack", persist(2))
        }

        val componentsNbt = new NBTTagList()
        for ((address, name) <- components) {
          val componentNbt = new NBTTagCompound()
          componentNbt.setString("address", address)
          componentNbt.setString("name", name)
          componentsNbt.appendTag(componentNbt)
        }
        nbt.setTag("components", componentsNbt)

        val signalsNbt = new NBTTagList()
        for (s <- signals.iterator) {
          val signalNbt = new NBTTagCompound()
          signalNbt.setString("name", s.name)
          signalNbt.setNewCompoundTag("args", args => {
            args.setInteger("length", s.args.length)
            s.args.zipWithIndex.foreach {
              case (Unit, i) => args.setByte("arg" + i, -1)
              case (arg: Boolean, i) => args.setByte("arg" + i, if (arg) 1 else 0)
              case (arg: Double, i) => args.setDouble("arg" + i, arg)
              case (arg: String, i) => args.setString("arg" + i, arg)
              case (arg: Array[Byte], i) => args.setByteArray("arg" + i, arg)
              case (arg: Map[String, String], i) =>
                val list = new NBTTagList()
                for ((key, value) <- arg) {
                  list.append(key)
                  list.append(value)
                }
                args.setTag("arg" + i, list)
              case (_, i) => args.setByte("arg" + i, -1)
            }
          })
          signalsNbt.appendTag(signalNbt)
        }
        nbt.setTag("signals", signalsNbt)

        rom.foreach(rom => nbt.setNewCompoundTag("rom", rom.save))
        tmp.foreach(tmp => nbt.setNewCompoundTag("tmp", tmp.save))

        nbt.setInteger("kernelMemory", math.ceil(kernelMemory / ramScale).toInt)
        nbt.setLong("timeStarted", timeStarted)
        nbt.setLong("cpuTime", cpuTime)
        nbt.setInteger("remainingPause", remainingPause)
        message.foreach(nbt.setString("message", _))
      } catch {
        case e: LuaRuntimeException =>
          OpenComputers.log.warning("Could not persist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
          nbt.removeTag("state")
      }

      // Limit memory again.
      recomputeMemory()
    }
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
    // Reset error state.
    message = None

    // Creates a new state with all base libraries and the persistence library
    // loaded into it. This means the state has much more power than it
    // rightfully should have, so we sandbox it a bit in the following.
    LuaStateFactory.createState() match {
      case None =>
        lua = null
        message = Some("native libraries not available")
        return false
      case Some(value) => lua = value
    }

    // Connect the ROM and `/tmp` node to our owner. We're not in a network in
    // case we're loading, which is why we have to check it here.
    if (node.network != null) {
      rom.foreach(rom => node.connect(rom.node))
      tmp.foreach(tmp => node.connect(tmp.node))
    }

    try {
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

      // Date formatting function.
      lua.pushScalaFunction(lua => {
        val format =
          if (lua.getTop > 0 && lua.isString(1)) lua.toString(1)
          else "%d/%m/%y %H:%M:%S"
        val time =
          if (lua.getTop > 1 && lua.isNumber(2)) lua.toNumber(2) * 1000 / 60 / 60
          else worldTime + 6000

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
        lua.pushNumber((worldTime + 6000) * 60 * 60 / 1000)
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
        lua.pushNumber((worldTime - timeStarted) / 20.0)
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
      lua.pushScalaFunction(lua => {
        lua.pushBoolean(isRobot)
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
        lua.pushBoolean(signal(lua.checkString(1), lua.toSimpleJavaObjects(2): _*))
        1
      })
      lua.setField(-2, "pushSignal")

      // And its ROM address.
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

      // User management.
      lua.pushScalaFunction(lua => {
        _users.foreach(lua.pushString)
        _users.size
      })
      lua.setField(-2, "users")

      lua.pushScalaFunction(lua => try {
        if (_users.size >= Settings.get.maxUsers)
          throw new Exception("too many users")

        val name = lua.checkString(1)

        if (_users.contains(name))
          throw new Exception("user exists")
        if (name.length > Settings.get.maxUsernameLength)
          throw new Exception("username too long")
        if (!MinecraftServer.getServer.getConfigurationManager.getAllUsernames.contains(name))
          throw new Exception("player must be online")

        _users.synchronized {
          _users += name
          usersChanged = true
        }
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
        val name = lua.checkString(1)
        _users.synchronized {
          val success = _users.remove(name)
          if (success) {
            usersChanged = true
          }
          lua.pushBoolean(success)
        }
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

      class LimitReachedException extends Exception

      lua.pushScalaFunction(lua => {
        val address = lua.checkString(1)
        val method = lua.checkString(2)
        val args = lua.toSimpleJavaObjects(3)
        try {
          (Option(node.network.node(address)) match {
            case Some(component: server.network.Component) if component.canBeSeenFrom(node) || component == node =>
              val direct = component.isDirect(method)
              if (direct) callCounts.synchronized {
                val limit = component.limit(method)
                val counts = callCounts.getOrElseUpdate(component.address, mutable.Map.empty[String, Int])
                val count = counts.getOrElseUpdate(method, 0)
                if (count >= limit) {
                  throw new LimitReachedException()
                }
                counts(method) += 1
              }
              component.invoke(method, this, args: _*)
            case _ => throw new Exception("no such component")
          }) match {
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
                if (true) {
                  lua.pushString(e.getStackTraceString)
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

      lua.setGlobal("component")

      initPerms()

      lua.load(classOf[Computer].getResourceAsStream(Settings.scriptPath + "kernel.lua"), "=kernel", "t")
      lua.newThread() // Left as the first value on the stack.

      // Clear any left-over signals from a previous run.
      signals.clear()

      return true
    }
    catch {
      case ex: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Failed initializing computer.", ex)
        close()
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

  private def close() = state.synchronized(
    if (state.size == 0 || state.top != Computer.State.Stopped) {
      state.clear()
      state.push(Computer.State.Stopped)
      if (lua != null) {
        lua.setTotalMemory(Integer.MAX_VALUE)
        lua.close()
      }
      lua = null
      kernelMemory = 0
      signals.clear()
      timeStarted = 0
      cpuTime = 0
      cpuStart = 0
      remainIdle = 0

      // Mark state change in owner, to send it to clients.
      owner.markAsChanged()
    })

  // ----------------------------------------------------------------------- //

  private def switchTo(value: Computer.State.Value) = {
    val result = state.pop()
    state.push(value)
    if (value == Computer.State.Yielded || value == Computer.State.SynchronizedReturn) {
      remainIdle = 0
      Computer.threadPool.submit(this)
    }

    // Mark state change in owner, to send it to clients.
    owner.markAsChanged()

    result
  }

  private def isGamePaused = !MinecraftServer.getServer.isDedicatedServer && (MinecraftServer.getServer match {
    case integrated: IntegratedServer => integrated.getServerListeningThread.isGamePaused
    case _ => false
  })

  // This is a really high level lock that we only use for saving and loading.
  override def run(): Unit = this.synchronized {
    val enterState = state.synchronized {
      if (state.top == Computer.State.Stopped ||
        state.top == Computer.State.Stopping ||
        state.top == Computer.State.Paused) {
        return
      }
      // See if the game appears to be paused, in which case we also pause.
      if (isGamePaused) {
        state.push(Computer.State.Paused)
        return
      }
      switchTo(Computer.State.Running)
    }

    try {
      // The kernel thread will always be at stack index one.
      assert(lua.isThread(1))

      if (Settings.get.activeGC) {
        // Help out the GC a little. The emergency GC has a few limitations
        // that will make it free less memory than doing a full step manually.
        lua.gc(LuaState.GcAction.COLLECT, 0)
      }

      // Resume the Lua state and remember the number of results we get.
      cpuStart = System.nanoTime()
      val (results, runtime) = enterState match {
        case Computer.State.SynchronizedReturn =>
          // If we were doing a synchronized call, continue where we left off.
          assert(lua.getTop == 2)
          assert(lua.isTable(2))
          (lua.resume(1, 1), System.nanoTime() - cpuStart)
        case Computer.State.Yielded =>
          if (kernelMemory == 0) {
            // We're doing the initialization run.
            if (lua.resume(1, 0) > 0) {
              // We expect to get nothing here, if we do we had an error.
              (0, 0L)
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
              (1, 0L)
            }
          }
          else (signals.synchronized(if (signals.isEmpty) None else Some(signals.dequeue())) match {
            case Some(signal) =>
              lua.pushString(signal.name)
              signal.args.foreach(arg => lua.pushValue(arg))
              lua.resume(1, 1 + signal.args.length)
            case _ =>
              lua.resume(1, 0)
          }, System.nanoTime() - cpuStart)
        case s => throw new AssertionError("Running computer from invalid state " + s.toString)
      }

      // Keep track of time spent executing the computer.
      cpuTime += runtime

      // Check if the kernel is still alive.
      state.synchronized(if (lua.status(1) == LuaState.YIELD) {
        // Check if someone called pause() or stop() in the meantime.
        state.top match {
          case Computer.State.Running =>
            // If we get one function it must be a wrapper for a synchronized
            // call. The protocol is that a closure is pushed that is then called
            // from the main server thread, and returns a table, which is in turn
            // passed to the originating coroutine.yield().
            if (results == 1 && lua.isFunction(2)) {
              switchTo(Computer.State.SynchronizedCall)
            }
            // Check if we are shutting down, and if so if we're rebooting. This
            // is signalled by boolean values, where `false` means shut down,
            // `true` means reboot (i.e shutdown then start again).
            else if (results == 1 && lua.isBoolean(2)) {
              if (lua.toBoolean(2)) switchTo(Computer.State.Restarting)
              else switchTo(Computer.State.Stopping)
            }
            else {
              // If we have a single number, that's how long we may wait before
              // resuming the state again. Note that the sleep may be interrupted
              // early if a signal arrives in the meantime. If we have something
              // else we just process the next signal or wait for one.
              val sleep =
                if (results == 1 && lua.isNumber(2)) (lua.toNumber(2) * 20).toInt
                else Int.MaxValue
              lua.pop(results)
              signals.synchronized {
                // Immediately check for signals to allow processing more than one
                // signal per game tick.
                if (signals.isEmpty && sleep > 0) {
                  switchTo(Computer.State.Sleeping)
                  remainIdle = sleep
                } else {
                  switchTo(Computer.State.Yielded)
                }
              }
            }
          case Computer.State.Paused =>
            state.pop() // Paused
            state.pop() // Running, no switchTo to avoid new future.
            state.push(Computer.State.Yielded)
            state.push(Computer.State.Paused)
          case Computer.State.Stopping => // Nothing to do, we'll die anyway.
          case _ => throw new AssertionError(
            "Invalid state in executor post-processing.")
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
          stop()
        }
        else {
          lua.setTotalMemory(Int.MaxValue)
          val error = lua.toString(3)
          if (error != null) crash(error)
          else crash("unknown error")
        }
      })
    }
    catch {
      case e: LuaRuntimeException =>
        OpenComputers.log.warning("Kernel crashed. This is a bug!\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        crash("kernel panic: this is a bug, check your log file and report it")
      case e: LuaGcMetamethodException =>
        if (e.getMessage != null) crash("kernel panic:\n" + e.getMessage)
        else crash("kernel panic:\nerror in garbage collection metamethod")
      case e: LuaMemoryAllocationException =>
        crash("not enough memory")
      case e: java.lang.Error if e.getMessage == "not enough memory" =>
        crash("not enough memory")
      case e: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Unexpected error in kernel. This is a bug!\n", e)
        crash("kernel panic: this is a bug, check your log file and report it")
    }
  }
}

object Computer {

  /** Signals are messages sent to the Lua state from Java asynchronously. */
  private class Signal(val name: String, val args: Array[Any])

  /** Possible states of the computer, and in particular its executor. */
  private object State extends Enumeration {
    /** The computer is not running right now and there is no Lua state. */
    val Stopped = Value("Stopped")

    /** Booting up, doing the first run to initialize the kernel and libs. */
    val Starting = Value("Starting")

    /** Computer is currently rebooting. */
    val Restarting = Value("Restarting")

    /** The computer is currently shutting down. */
    val Stopping = Value("Stopping")

    /** The computer is paused and waiting for the game to resume. */
    val Paused = Value("Paused")

    /** The computer executor is waiting for a synchronized call to be made. */
    val SynchronizedCall = Value("SynchronizedCall")

    /** The computer should resume with the result of a synchronized call. */
    val SynchronizedReturn = Value("SynchronizedReturn")

    /** The computer will resume as soon as possible. */
    val Yielded = Value("Yielded")

    /** The computer is yielding for a longer amount of time. */
    val Sleeping = Value("Sleeping")

    /** The computer is up and running, executing Lua code. */
    val Running = Value("Running")
  }

  private val threadPool = ThreadPoolFactory.create("Lua", Settings.get.threads)

}