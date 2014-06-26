package li.cil.oc.util

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.channels.Channels

import com.naef.jnlua
import com.naef.jnlua.LuaState
import com.naef.jnlua.NativeSupport.Loader
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.util.ExtendedLuaState._
import li.cil.oc.{OpenComputers, Settings}
import org.apache.commons.lang3.SystemUtils
import org.apache.logging.log4j.Level

import scala.util.Random
import scala.util.control.Breaks._

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

  private var isWindows = false

  private var _is64Bit = false

  def isAvailable = haveNativeLibrary

  def is64Bit = _is64Bit

  // Since we use native libraries we have to do some work. This includes
  // figuring out what we're running on, so that we can load the proper shared
  // libraries compiled for that system. It also means we have to unpack the
  // shared libraries somewhere so that we can load them, because we cannot
  // load them directly from a JAR.
  breakable {
    // See http://lopica.sourceforge.net/os.html
    val architecture =
      System.getProperty("sun.arch.data.model") match {
        case "32" => "32"
        case "64" => "64"
        case _ =>
          System.getProperty("os.arch").toLowerCase match {
            case "i386" | "x86" => "32"
            case "amd64" | "x86_64" => "64"
            case "ppc" | "powerpc" => "ppc"
            case _ =>
              OpenComputers.log.warn("Unsupported architecture, you won't be able to host games with working computers.")
              break()
          }
      }
    _is64Bit = architecture == "64"

    val extension = System.getProperty("os.name").toLowerCase match {
      case name if name.startsWith("linux") => ".so"
      case name if name.startsWith("mac") => ".dylib"
      case name if name.startsWith("windows") => ".dll"
      case name if name.contains("bsd") => ".bsd.so"
      case _ =>
        OpenComputers.log.warn("Unsupported operating system, you won't be able to host games with working computers.")
        break()
    }
    isWindows = extension == ".dll"
    val libPath = "/assets/" + Settings.resourceDomain + "/lib/"

    if (isWindows && !Settings.get.alwaysTryNative) {
      if (SystemUtils.IS_OS_WINDOWS_XP) {
        OpenComputers.log.warn("Sorry, but Windows XP isn't supported. I'm afraid you'll have to use a newer Windows. I very much recommend upgrading your Windows, anyway, since Microsoft will stop supporting Windows XP in April 2014.")
        break()
      }

      if (SystemUtils.IS_OS_WINDOWS_2003) {
        OpenComputers.log.warn("Sorry, but Windows Server 2003 isn't supported. I'm afraid you'll have to use a newer Windows.")
        break()
      }
    }

    val tmpPath = {
      val path = System.getProperty("java.io.tmpdir")
      if (path.endsWith("/") || path.endsWith("\\")) path
      else path + "/"
    }

    val library = "native." + architecture + extension
    val libraryUrl = classOf[Machine].getResource(libPath + library)
    if (libraryUrl == null) {
      OpenComputers.log.warn("Unsupported platform, you won't be able to host games with working computers.")
      break()
    }

    // Found file with proper extension. Create a temporary file.
    val file = new File(tmpPath + "OpenComputersMod-" + OpenComputers.Version + library)
    // If the file, already exists, make sure it's the same we need, if it's
    // not disable use of the natives.
    if (file.exists()) {
      val inCurrent = libraryUrl.openStream()
      val inExisting = new FileInputStream(file)
      var matching = true
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
          OpenComputers.log.error("Could not update native library, is another instance of Minecraft with an older version of the mod already running?")
        break()
      }
    }
    }
    // Copy the file contents to the temporary file.
    try {
      val in = Channels.newChannel(libraryUrl.openStream())
      val out = new FileOutputStream(file).getChannel
      out.transferFrom(in, 0, Long.MaxValue)
      in.close()
      out.close()
      file.deleteOnExit()
      // Set file permissions more liberally for multi-user+instance servers.
      file.setReadable(true, false)
      file.setWritable(true, false)
      file.setExecutable(true, false)
    }
    catch {
      // Java (or Windows?) locks the library file when opening it, so any
      // further tries to update it while another instance is still running
      // will fail. We still want to try each time, since the files may have
      // been updated.
      case t: Throwable => // Nothing.
    }

    // Remember the temporary file's location for the loader.
    val libraryPath = file.getAbsolutePath

    // Register a custom library loader with JNLua to actually load the ones we
    // just extracted.
    jnlua.NativeSupport.getInstance().setLoader(new Loader {
      def load() {
        try {
          System.load(libraryPath)
        } catch {
          case t: Throwable =>
            haveNativeLibrary = false
            throw t
        }
      }
    })

    haveNativeLibrary = true
  }

  // Try creating a state once, to verify the libs are working.
  createState() match {
    case Some(state) => state.close()
    case _ => haveNativeLibrary = false
  }

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
          lua.getTop match {
            case 0 => lua.pushNumber(random.nextDouble())
            case 1 =>
              val u = lua.checkNumber(1).toInt
              lua.checkArg(1, 1 <= u, "interval is empty")
              lua.pushInteger(1 + random.nextInt(u))
            case 2 =>
              val l = lua.checkNumber(1).toInt
              val u = lua.checkNumber(2).toInt
              lua.checkArg(1, l <= u, "interval is empty")
              lua.pushInteger(l + random.nextInt(u - (l - 1)))
            case _ => throw new IllegalArgumentException("wrong number of arguments")
          }
          1
        })
        state.setField(-2, "random")

        state.pushScalaFunction(lua => {
          random.setSeed(lua.checkInteger(1))
          0
        })
        state.setField(-2, "randomseed")

        // Pop the math table.
        state.pop(1)

        return Some(state)
      }
      catch {
        case t: Throwable =>
          OpenComputers.log.log(Level.WARN, "Failed creating Lua state.", t)
          state.close()
      }
    }
    catch {
      case _: UnsatisfiedLinkError =>
        OpenComputers.log.error("Failed loading the native libraries.")
      case t: Throwable =>
        OpenComputers.log.log(Level.WARN, "Failed creating Lua state.", t)
    }
    None
  }
}