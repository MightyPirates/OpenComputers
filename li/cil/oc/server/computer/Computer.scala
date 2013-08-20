package li.cil.oc.server.computer

import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels

import scala.collection.mutable.Map

import com.naef.jnlua.JavaFunction
import com.naef.jnlua.LuaState
import com.naef.jnlua.NativeSupport
import com.naef.jnlua.NativeSupport.Loader

import net.minecraft.nbt.NBTTagCompound

object ComputerRegistry {
  val driverApis = "oc_apis"
  val plutoPersist = "pluto_persist"
  val plutoUnpersist = "pluto_unpersist"
  val plutoUnpersistTable = "pluto_unpersistTable"
  val plutoPersistTable = "pluto_persistTable"
}

class Computer(val owner: AnyRef) extends IComputerContext {
  // ----------------------------------------------------------------------- //
  // Initialization
  // ----------------------------------------------------------------------- //

  private val lua = LuaStateFactory.createState()

  lua.getGlobal("pluto")
  lua.getField(-1, "persist")
  lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoPersist)
  lua.getField(-1, "unpersist")
  lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoUnpersist)
  lua.pop(1)

  lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/sandbox.lua"), "sandbox")
  lua.call(0, 0)

  Drivers.injectInto(this)

  lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/pluto.lua"), "pluto")
  lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.driverApis)
  lua.pushJavaFunction(new JavaFunction() {
    def invoke(lua: LuaState): Int = {
      println(lua.toString(1))
      return 0
    }
  })
  lua.call(2, 2)
  lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoUnpersistTable)
  lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoPersistTable)

  lua.gc(LuaState.GcAction.COLLECT, 0)
  private val osMemory = lua.getTotalMemory() - lua.getFreeMemory()
  lua.setTotalMemory(osMemory + 64 * 1024)

  //writeToNBT(new NBTTagCompound())
  
  println("OS uses " + osMemory + " bytes of memory.")

  // TODO Return game time from os.time().
  //  lua.getGlobal("os")
  //  lua.pushJavaFunction(new JavaFunction() {
  //    def invoke(state: LuaState): Int = {
  //      state.pushNumber(0)
  //      return 1
  //    }
  //  })
  //  lua.setField(1, "time")

  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  def luaState = lua

  def update() {
    if (lua.getTop() == 0) {
      println("Loading OS!")
      lua.load(classOf[Computer].getResourceAsStream("/assets/opencomputers/lua/os.lua"), "os")
      lua.newThread()
    }
    val nres = lua.resume(1, 0)
    val i = lua.toInteger(-1)
    lua.pop(nres)
    println(i)
  }

  // ----------------------------------------------------------------------- //
  // Saving / Loading
  // ----------------------------------------------------------------------- //

  def readFromNBT(nbt: NBTTagCompound) = {
    val state = nbt.getString("state")
    lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoUnpersist)
    lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoUnpersistTable)
    lua.pushString(state)
    lua.call(2, 1)
    lua.setField(LuaState.GLOBALSINDEX, "_G")
  }

  def writeToNBT(nbt: NBTTagCompound) = {
    lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoPersist)
    lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.plutoPersistTable)
    lua.pushValue(LuaState.GLOBALSINDEX)
    lua.call(2, 1)
    val state = lua.toString(-1)
    lua.pop(1)
    nbt.setString("state", state)
  }
}

/**
 * Factory singleton used to spawn new LuaState instances.
 *
 * This is realized as a singleton so that we only have to resolve shared
 * library references once during initialization and can then re-use the
 * already loaded ones.
 */
object LuaStateFactory {
  // ----------------------------------------------------------------------- //
  // Initialization
  // ----------------------------------------------------------------------- //

  private val libraries = Map.empty[String, String]
  private val basePath = "/assets/opencomputers/"

  // Since we use native libraries we have to do some work. This includes
  // figuring out what we're running on, so that we can load the proper shared
  // libraries compiled for that system. It also means we have to unpack the
  // shared libraries somewhere so that we can load them, because we cannot
  // load them directly from a JAR.
  {
    val platform = System.getProperty("os.name").toLowerCase() match {
      case name if (name.startsWith("linux")) => "linux"
      case name if (name.startsWith("windows")) => "windows"
      case name if (name.startsWith("mac")) => "mac"
    }
    val libPath = basePath + "lib/" + System.getProperty("os.arch") + "/" + platform + "/"

    val libExt = platform match {
      case "linux" => ".so"
      case "windows" => ".dll"
      case "mac" => ".dylib"
    }
    val tmpPath = {
      val path = System.getProperty("java.io.tmpdir")
      if (path.endsWith("/") || path.endsWith("\\")) path
      else path + "/"
    }

    for (library <- Array("lua5.1", "jnlua5.1", "pluto")) {
      val libraryUrl = classOf[Computer].getResource(libPath + library + libExt)
      if (libraryUrl == null) {
        throw new NotImplementedError("Unsupported platform.")
      }
      // Found file with proper extension. Create a temporary file.
      val file = new File(tmpPath + library + libExt)
      if (!file.exists()) {
        file.deleteOnExit()
        // Copy the file contents to the temporary file.
        val in = Channels.newChannel(libraryUrl.openStream())
        val out = new FileOutputStream(file).getChannel()
        out.transferFrom(in, 0, Long.MaxValue)
        in.close()
        out.close()
      }
      // Remember the temporary file's location for later.
      libraries += library -> file.getAbsolutePath()
    }

    // Register a custom library loader with JNLua to actually load the ones we
    // just extracted.
    NativeSupport.getInstance().setLoader(new Loader {
      def load() {
        System.load(libraries("lua5.1"))
        System.load(libraries("jnlua5.1"))
      }
    })
  }

  // ----------------------------------------------------------------------- //
  // Factory
  // ----------------------------------------------------------------------- //

  def createState(): LuaState = {
    val state = new LuaState(Integer.MAX_VALUE)
    try {
      // Load all libraries.
      state.openLibs()

      // Adjust the path in which Lua will look for the pluto library. Because
      // the only external library we'll load is the pluto library we can get
      // away with just setting the path to the actual library file...
      state.getGlobal("package")
      state.pushString(libraries("pluto"))
      state.setField(1, "cpath")
      state.pop(1)

      // Load the pluto library. We do this via the Lua facilities because we
      // cannot directly reference the Lua state pointer JNLua uses.
      state.getGlobal("require")
      state.pushString("pluto")
      state.call(1, 0)
    }
    catch {
      case ex: Throwable => {
        ex.printStackTrace()
        state.close()
        return null
      }
    }
    return state
  }
}