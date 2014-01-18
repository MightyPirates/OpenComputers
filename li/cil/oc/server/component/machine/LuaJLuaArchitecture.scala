package li.cil.oc.server.component.machine

import li.cil.oc.server.component.Machine
import net.minecraft.nbt.NBTTagCompound
import org.luaj.vm2.lib.jse.{CoerceJavaToLua, JsePlatform}
import li.cil.oc.server.component.Machine.State
import org.luaj.vm2.{LuaFunction, LuaValue, Globals}
import li.cil.oc.util.{GameTimeFormatter, LuaStateFactory}
import li.cil.oc.{OpenComputers, server, Settings}
import java.util.logging.Level
import java.io.{IOException, FileNotFoundException}

class LuaJLuaArchitecture(machine: Machine) extends Architecture {
  private var lua: Globals = _

  // ----------------------------------------------------------------------- //

  private def node = machine.node

  private def state = machine.state

  private def components = machine.components

  // ----------------------------------------------------------------------- //

  def isInitialized = ???

  def recomputeMemory() {}

  // ----------------------------------------------------------------------- //

  def runSynchronized() {
    try {
      lua.set(0, lua.get(0).call())
    }
    catch {
      case _: OutOfMemoryError =>
        // This can happen if we run out of memory while converting a Java
        // exception to a string (which we have to do to avoid keeping
        // userdata on the stack, which cannot be persisted).
        throw new java.lang.OutOfMemoryError("not enough memory")
    }
  }

  def runThreaded(enterState: State.Value) = ???

  // ----------------------------------------------------------------------- //

  def init() =  {
    lua = JsePlatform.standardGlobals()
    CoerceJavaToLua.coerce()
    val os = lua.get("os")
    os.set("clock", () => (machine.cpuTime + (System.nanoTime() - machine.cpuStart)) * 10e-10)

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
      lua.pushNumber((machine.worldTime - machine.timeStarted) / 20.0)
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
      lua.pushBoolean(machine.isRobot)
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
      machine.rom.foreach(rom => Option(rom.node.address) match {
        case None => lua.pushNil()
        case Some(address) => lua.pushString(address)
      })
      1
    })
    lua.setField(-2, "romAddress")

    // And it's /tmp address...
    lua.pushScalaFunction(lua => {
      machine.tmp.foreach(tmp => Option(tmp.node.address) match {
        case None => lua.pushNil()
        case Some(address) => lua.pushString(address)
      })
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

    lua.pushScalaFunction(lua => {
      val address = lua.checkString(1)
      val method = lua.checkString(2)
      val args = lua.toSimpleJavaObjects(3)
      try {
        machine.invoke(address, method, args) match {
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
          if (Settings.get.logLuaCallbackErrors && !e.isInstanceOf[Machine.LimitReachedException]) {
            OpenComputers.log.log(Level.WARNING, "Exception in Lua callback.", e)
          }
          e match {
            case _: Machine.LimitReachedException =>
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

    lua.setGlobal("component")

    initPerms()

    lua.load(classOf[Machine].getResourceAsStream(Settings.scriptPath + "kernel.lua"), "=kernel", "t")
    lua.newThread() // Left as the first value on the stack.

    true
  }

  def close() = ???

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {}

  def save(nbt: NBTTagCompound) {}
}
