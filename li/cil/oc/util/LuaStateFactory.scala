package li.cil.oc.util

import com.naef.jnlua.NativeSupport.Loader
import com.naef.jnlua.{LuaState, NativeSupport}
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.util.logging.Level
import li.cil.oc.server.component.Computer
import li.cil.oc.util.ExtendedLuaState._
import li.cil.oc.{OpenComputers, Settings}
import org.lwjgl.LWJGLUtil
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
              OpenComputers.log.warning("Unsupported architecture, you won't be able to host games with working computers.")
              break()
          }
      }
    _is64Bit = architecture == "64"

    val extension = try {
      LWJGLUtil.getPlatform match {
        case LWJGLUtil.PLATFORM_LINUX => ".so"
        case LWJGLUtil.PLATFORM_MACOSX => ".dylib"
        case LWJGLUtil.PLATFORM_WINDOWS => ".dll"
        case _ =>
          OpenComputers.log.warning("Unsupported operating system, you won't be able to host games with working computers.")
          break()
      }
    }
    catch {
      // Dedicated server doesn't necessarily have LWJGLUtil...
      case _: NoClassDefFoundError =>
        System.getProperty("os.name").toLowerCase match {
          case name if name.startsWith("linux") => ".so"
          case name if name.startsWith("mac") => ".dylib"
          case name if name.startsWith("windows") => ".dll"
          case _ =>
            OpenComputers.log.warning("Unsupported operating system, you won't be able to host games with working computers.")
            break()
        }
    }
    isWindows = extension == ".dll"
    val libPath = "/assets/" + Settings.resourceDomain + "/lib/"

    val tmpPath = {
      val path = System.getProperty("java.io.tmpdir")
      if (path.endsWith("/") || path.endsWith("\\")) path
      else path + "/"
    }

    val library = "native." + architecture + extension
    val libraryUrl = classOf[Computer].getResource(libPath + library)
    if (libraryUrl == null) {
      OpenComputers.log.warning("Unsupported platform, you won't be able to host games with working computers.")
      break()
    }

    // Found file with proper extension. Create a temporary file.
    val file = new File(tmpPath + "OpenComputersMod-" + library)
    // Try to delete an old instance of the library, in case we have an update
    // and deleteOnExit fails (which it regularly does on Windows it seems).
    try {
      file.delete()
    }
    catch {
      case t: Throwable => // Ignore.
    }
    // Copy the file contents to the temporary file.
    try {
      val in = Channels.newChannel(libraryUrl.openStream())
      val out = new FileOutputStream(file).getChannel
      out.transferFrom(in, 0, Long.MaxValue)
      in.close()
      out.close()
      file.deleteOnExit()
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
    NativeSupport.getInstance().setLoader(new Loader {
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

  // ----------------------------------------------------------------------- //
  // Factory
  // ----------------------------------------------------------------------- //

  def createState(): Option[LuaState] = {
    if (!haveNativeLibrary) return None

    try {
      val state = new LuaState(Int.MaxValue)
      try {
        // Load all libraries.
        state.openLib(LuaState.Library.BASE)
        state.openLib(LuaState.Library.BIT32)
        state.openLib(LuaState.Library.COROUTINE)
        state.openLib(LuaState.Library.DEBUG)
        state.openLib(LuaState.Library.ERIS)
        state.openLib(LuaState.Library.MATH)
        state.openLib(LuaState.Library.STRING)
        state.openLib(LuaState.Library.TABLE)
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
              val u = lua.checkInteger(1)
              lua.checkArg(1, 1 < u, "interval is empty")
              lua.pushInteger(1 + random.nextInt(u))
            case 2 =>
              val l = lua.checkInteger(1)
              val u = lua.checkInteger(2)
              lua.checkArg(1, l < u, "interval is empty")
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

        // Provide some better Unicode support.
        state.newTable()

        // TODO find (probably not necessary?)

        // TODO format (probably not necessary?)

        // TODO gmatch (probably not necessary?)

        // TODO gsub (probably not necessary?)

        // TODO match (probably not necessary?)

        state.pushScalaFunction(lua => {
          lua.pushString(lua.checkString(1).toLowerCase)
          1
        })
        state.setField(-2, "lower")

        state.pushScalaFunction(lua => {
          lua.pushString(lua.checkString(1).toUpperCase)
          1
        })
        state.setField(-2, "upper")

        state.pushScalaFunction(lua => {
          lua.pushString(String.valueOf((1 to lua.getTop).map(lua.checkInteger).map(_.toChar).toArray))
          1
        })
        state.setField(-2, "char")

        state.pushScalaFunction(lua => {
          lua.pushInteger(lua.checkString(1).length)
          1
        })
        state.setField(-2, "len")

        state.pushScalaFunction(lua => {
          lua.pushString(lua.checkString(1).reverse)
          1
        })
        state.setField(-2, "reverse")

        state.pushScalaFunction(lua => {
          val string = lua.checkString(1)
          val start = math.max(0, lua.checkInteger(2) match {
            case i if i < 0 => string.length + i
            case i => i - 1
          })
          val end =
            if (lua.getTop > 2) math.min(string.length, lua.checkInteger(3) match {
              case i if i < 0 => string.length + i + 1
              case i => i
            })
            else string.length
          if (end <= start) lua.pushString("")
          else lua.pushString(string.substring(start, end))
          1
        })
        state.setField(-2, "sub")

        state.setGlobal("unicode")

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
        if (isWindows) {
          OpenComputers.log.severe(
            "Please ensure you have the Visual C++ 2012 Runtime installed " +
              "(when on 64 Bit, both the 32 bit and 64 bit version of the " +
              "runtime).")
        }
      case t: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Failed creating Lua state.", t)
    }
    None
  }
}