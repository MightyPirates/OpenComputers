package li.cil.oc.util

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.channels.Channels
import java.util.logging.Level

import com.naef.jnlua
import com.naef.jnlua.LuaState
import com.naef.jnlua.NativeSupport.Loader
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.util.ExtendedLuaState._
import li.cil.oc.{OpenComputers, Settings}
import org.apache.commons.lang3.SystemUtils

import scala.util.Random

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

  /** Set to true in initialization code below if available. */
  private var haveNativeLibrary = false

  private val isWindows = SystemUtils.IS_OS_WINDOWS

  private var _is64Bit = false

  private var currentLib = ""

  def isAvailable = haveNativeLibrary

  def is64Bit = _is64Bit

  // Register a custom library loader with JNLua. We have to trigger
  // library loads through JNLua to ensure the LuaState class is the
  // one loading the library and not the other way around - the native
  // library also references the LuaState class, and if it is loaded
  // that way, it will fail to access native methods in its static
  // initializer, because the native lib will not have been completely
  // loaded at the time the initializer runs.
  jnlua.NativeSupport.getInstance().setLoader(new Loader {
    def load() {
      System.load(currentLib)
    }
  })

  // Since we use native libraries we have to do some work. This includes
  // figuring out what we're running on, so that we can load the proper shared
  // libraries compiled for that system. It also means we have to unpack the
  // shared libraries somewhere so that we can load them, because we cannot
  // load them directly from a JAR.
  def init() {
    if (isWindows && !Settings.get.alwaysTryNative) {
      if (SystemUtils.IS_OS_WINDOWS_XP) {
        OpenComputers.log.warning("Sorry, but Windows XP isn't supported. I'm afraid you'll have to use a newer Windows. I very much recommend upgrading your Windows, anyway, since Microsoft has stopped supporting Windows XP in April 2014.")
        return
      }

      if (SystemUtils.IS_OS_WINDOWS_2003) {
        OpenComputers.log.warning("Sorry, but Windows Server 2003 isn't supported. I'm afraid you'll have to use a newer Windows.")
        return
      }
    }

    val sunArch = System.getProperty("sun.arch.data.model")
    val osArch = System.getProperty("os.arch").toLowerCase
    _is64Bit = sunArch == "64" || osArch == "amd64" || osArch == "x86_64"

    val libPath = "/assets/" + Settings.resourceDomain + "/lib/"
    val libNames = Array(
      "native.64.dll",
      "native.64.dylib",
      "native.64.so",
      "native.64.bsd.so",
      "native.32.dll",
      "native.32.dylib",
      "native.32.so",
      "native.32.bsd.so",
      "native.32.arm.so"
    )
    val tmpPath = {
      val path = System.getProperty("java.io.tmpdir")
      if (path.endsWith("/") || path.endsWith("\\")) path
      else path + "/"
    }

    // Try to find a working lib.
    for (library <- libNames if !haveNativeLibrary) {
      OpenComputers.log.fine(s"Trying native library '$library'...")
      val libraryUrl = classOf[Machine].getResource(libPath + library)
      if (libraryUrl != null) {
        // Create a temporary file.
        val file = new File(tmpPath + "OpenComputersMod-" + OpenComputers.Version + "-" + library)
        // If the file, already exists, make sure it's the same we need, if it's
        // not disable use of the natives.
        if (file.exists()) {
          var matching = true
          try {
            val inCurrent = libraryUrl.openStream()
            val inExisting = new FileInputStream(file)
            var inCurrentByte = 0
            var inExistingByte = 0
            do {
              inCurrentByte = inCurrent.read()
              inExistingByte = inExisting.read()
              if (inCurrentByte != inExistingByte) {
                matching = false
                inCurrentByte = -1
                inExistingByte = -1
              }
            }
            while (inCurrentByte != -1 && inExistingByte != -1)
            inCurrent.close()
            inExisting.close()
          }
          catch {
            case _: Throwable =>
              matching = false
          }
          if (!matching) {
            // Try to delete an old instance of the library, in case we have an update
            // and deleteOnExit fails (which it regularly does on Windows it seems).
            // Note that this should only ever be necessary for dev-builds, where the
            // version number didn't change (since the version number is part of the name).
            try {
              file.delete()
            }
            catch {
              case t: Throwable => // Ignore.
            }
            if (file.exists()) {
              OpenComputers.log.severe(s"Could not update native library '${file.getName}'!")
            }
          }
        }
        // Copy the file contents to the temporary file.
        try {
          val in = Channels.newChannel(libraryUrl.openStream())
          try {
            val out = new FileOutputStream(file).getChannel
            try {
              out.transferFrom(in, 0, Long.MaxValue)
              file.deleteOnExit()
              // Set file permissions more liberally for multi-user+instance servers.
              file.setReadable(true, false)
              file.setWritable(true, false)
              file.setExecutable(true, false)
            }
            finally {
              out.close()
            }
          }
          finally {
            in.close()
          }
        }
        catch {
          // Java (or Windows?) locks the library file when opening it, so any
          // further tries to update it while another instance is still running
          // will fail. We still want to try each time, since the files may have
          // been updated.
          // Alternatively, the file could not be opened for reading/writing.
          case t: Throwable => // Nothing.
        }
        // Try to load the lib.
        currentLib = file.getAbsolutePath
        try {
          LuaState.initializeNative()
          new jnlua.LuaState().close()
          OpenComputers.log.info(s"Found a compatible native library: '${file.getName}'.")
          haveNativeLibrary = true
        }
        catch {
          case _: Throwable =>
            OpenComputers.log.log(Level.FINE, s"Could not load native library '${file.getName}'.")
            file.delete()
        }
      }
    }

    if (!haveNativeLibrary) {
      OpenComputers.log.warning("Unsupported platform, you won't be able to host games with persistent computers.")
    }
  }

  init()

  // ----------------------------------------------------------------------- //
  // Factory
  // ----------------------------------------------------------------------- //

  def createState(): Option[LuaState] = {
    if (!haveNativeLibrary) return None

    try {
      val state =
        if (Settings.get.limitMemory) new jnlua.LuaState(Int.MaxValue)
        else new jnlua.LuaState()
      try {
        // Load all libraries.
        state.openLib(jnlua.LuaState.Library.BASE)
        state.openLib(jnlua.LuaState.Library.BIT32)
        state.openLib(jnlua.LuaState.Library.COROUTINE)
        state.openLib(jnlua.LuaState.Library.DEBUG)
        state.openLib(jnlua.LuaState.Library.ERIS)
        state.openLib(jnlua.LuaState.Library.MATH)
        state.openLib(jnlua.LuaState.Library.STRING)
        state.openLib(jnlua.LuaState.Library.TABLE)
        state.pop(8)

        if (!Settings.get.hardwareSandbox) {
          state.openLib(jnlua.LuaState.Library.IO)
          state.openLib(jnlua.LuaState.Library.JAVA)
          state.openLib(jnlua.LuaState.Library.OS)
          state.openLib(jnlua.LuaState.Library.PACKAGE)
          state.pop(4)

          state.newTable()
          state.getGlobal("os")
          state.setField(-2, "os")

          state.getGlobal("loadfile")
          state.setField(-2, "loadfile")

          state.getGlobal("dofile")
          state.setField(-2, "dofile")

          state.setGlobal("native")

        }

        // Prepare table for os stuff.
        state.newTable()
        state.setGlobal("os")

        // Kill compat entries.
        state.pushNil()
        state.setGlobal("unpack")
        state.pushNil()
        state.setGlobal("loadstring")
        state.getGlobal("math")
        state.pushNil()
        state.setField(-2, "log10")
        state.pop(1)
        state.getGlobal("table")
        state.pushNil()
        state.setField(-2, "maxn")
        state.pop(1)

        // Remove some other functions we don't need and are dangerous.
        state.pushNil()
        state.setGlobal("dofile")
        state.pushNil()
        state.setGlobal("loadfile")

        state.getGlobal("math")

        // We give each Lua state it's own randomizer, since otherwise they'd
        // use the good old rand() from C. Which can be terrible, and isn't
        // necessarily thread-safe.
        val random = new Random
        state.pushScalaFunction(lua => {
          val r = random.nextDouble()
          lua.getTop match {
            case 0 => lua.pushNumber(r)
            case 1 =>
              val u = lua.checkNumber(1)
              lua.checkArg(1, 1 <= u, "interval is empty")
              lua.pushNumber(math.floor(r * u) + 1)
            case 2 =>
              val l = lua.checkNumber(1)
              val u = lua.checkNumber(2)
              lua.checkArg(2, l <= u, "interval is empty")
              lua.pushNumber(math.floor(r * (u - l + 1)) + l)
            case _ => throw new IllegalArgumentException("wrong number of arguments")
          }
          1
        })
        state.setField(-2, "random")

        state.pushScalaFunction(lua => {
          random.setSeed(lua.checkNumber(1).toLong)
          0
        })
        state.setField(-2, "randomseed")

        // Pop the math table.
        state.pop(1)

        return Some(state)
      }
      catch {
        case t: Throwable =>
          OpenComputers.log.log(Level.WARNING, "Failed creating Lua state.", t)
          state.close()
      }
    }
    catch {
      case _: UnsatisfiedLinkError =>
        OpenComputers.log.severe("Failed loading the native libraries.")
      case t: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Failed creating Lua state.", t)
    }
    None
  }
}