package li.cil.oc.server.machine.luac

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.util.regex.Pattern

import com.google.common.base.Strings
import com.google.common.io.PatternFilenameFilter
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Architecture
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.ExtendedLuaState._
import li.cil.repack.com.naef.jnlua
import li.cil.repack.com.naef.jnlua.NativeSupport.Loader
import net.minecraft.item.ItemStack
import org.apache.commons.lang3.SystemUtils

import scala.util.Random

object LuaStateFactory {
  def isAvailable: Boolean = {
    // Force initialization of both.
    val lua52 = Lua52.isAvailable
    val lua53 = Lua53.isAvailable
    lua52 || lua53
  }

  def luajRequested: Boolean = Settings.get.forceLuaJ || Settings.get.registerLuaJArchitecture

  def includeLuaJ: Boolean = !isAvailable || luajRequested

  def include52: Boolean = Lua52.isAvailable && !Settings.get.forceLuaJ

  def include53: Boolean = Lua53.isAvailable && Settings.get.enableLua53 && !Settings.get.forceLuaJ

  def default53: Boolean = include53 && Settings.get.defaultLua53

  def setDefaultArch(stack: ItemStack): ItemStack = {
    if (default53) {
      val lua53: Class[_ <: Architecture] = classOf[NativeLua53Architecture]
      Option(api.Driver.driverFor(stack)).foreach{
        case driver: api.driver.item.MutableProcessor => {
          driver.setArchitecture(stack, lua53)
        }
        case _ =>
      }
    }
    stack
  }

  object Lua52 extends LuaStateFactory {
    override def version: String = "lua52"

    override protected def create(maxMemory: Option[Int]) = maxMemory.fold(new jnlua.LuaState())(new jnlua.LuaState(_))

    override protected def openLibs(state: jnlua.LuaState): Unit = {
      state.openLib(jnlua.LuaState.Library.BASE)
      state.openLib(jnlua.LuaState.Library.BIT32)
      state.openLib(jnlua.LuaState.Library.COROUTINE)
      state.openLib(jnlua.LuaState.Library.DEBUG)
      state.openLib(jnlua.LuaState.Library.ERIS)
      state.openLib(jnlua.LuaState.Library.MATH)
      state.openLib(jnlua.LuaState.Library.STRING)
      state.openLib(jnlua.LuaState.Library.TABLE)
      state.pop(8)
    }
  }

  object Lua53 extends LuaStateFactory {
    override def version: String = "lua53"

    override protected def create(maxMemory: Option[Int]) = maxMemory.fold(new jnlua.LuaStateFiveThree())(new jnlua.LuaStateFiveThree(_))

    override protected def openLibs(state: jnlua.LuaState): Unit = {
      state.openLib(jnlua.LuaState.Library.BASE)
      state.openLib(jnlua.LuaState.Library.COROUTINE)
      state.openLib(jnlua.LuaState.Library.DEBUG)
      state.openLib(jnlua.LuaState.Library.ERIS)
      state.openLib(jnlua.LuaState.Library.MATH)
      state.openLib(jnlua.LuaState.Library.STRING)
      state.openLib(jnlua.LuaState.Library.TABLE)
      state.openLib(jnlua.LuaState.Library.UTF8)
      state.pop(8)
    }
  }

}

/**
 * Factory singleton used to spawn new LuaState instances.
 *
 * This is realized as a singleton so that we only have to resolve shared
 * library references once during initialization and can then re-use the
 * already loaded ones.
 */
abstract class LuaStateFactory {
  def version: String

  // ----------------------------------------------------------------------- //
  // Initialization
  // ----------------------------------------------------------------------- //

  /** Set to true in initialization code below if available. */
  private var haveNativeLibrary = false

  private var currentLib = ""

  private val libraryName = {
    if (!Strings.isNullOrEmpty(Settings.get.forceNativeLib)) Settings.get.forceNativeLib

    else if (SystemUtils.IS_OS_FREE_BSD && Architecture.IS_OS_X64) "native.64.bsd.so"
    else if (SystemUtils.IS_OS_FREE_BSD && Architecture.IS_OS_X86) "native.32.bsd.so"

    else if (SystemUtils.IS_OS_LINUX && Architecture.IS_OS_ARM) "native.32.arm.so"
    else if (SystemUtils.IS_OS_LINUX && Architecture.IS_OS_A64) "native.64.arm.so"
    else if (SystemUtils.IS_OS_LINUX && Architecture.IS_OS_X64) "native.64.so"
    else if (SystemUtils.IS_OS_LINUX && Architecture.IS_OS_X86) "native.32.so"

    else if (SystemUtils.IS_OS_MAC && Architecture.IS_OS_X64) "native.64.dylib"
    else if (SystemUtils.IS_OS_MAC && Architecture.IS_OS_X86) "native.32.dylib"

    else if (SystemUtils.IS_OS_WINDOWS && Architecture.IS_OS_X64) "native.64.dll"
    else if (SystemUtils.IS_OS_WINDOWS && Architecture.IS_OS_X86) "native.32.dll"

    else null
  }

  // Register a custom library loader with JNLua. We have to trigger
  // library loads through JNLua to ensure the LuaState class is the
  // one loading the library and not the other way around - the native
  // library also references the LuaState class, and if it is loaded
  // that way, it will fail to access native methods in its static
  // initializer, because the native lib will not have been completely
  // loaded at the time the initializer runs.
  private def prepareLoad(lib: String): Unit = jnlua.NativeSupport.getInstance().setLoader(new Loader {
    def load(): Unit = {
      System.load(lib)
    }
  })

  protected def create(maxMemory: Option[Int] = None): jnlua.LuaState

  protected def openLibs(state: jnlua.LuaState): Unit

  // ----------------------------------------------------------------------- //

  def isAvailable = haveNativeLibrary

  val is64Bit = Architecture.IS_OS_X64

  // Since we use native libraries we have to do some work. This includes
  // figuring out what we're running on, so that we can load the proper shared
  // libraries compiled for that system. It also means we have to unpack the
  // shared libraries somewhere so that we can load them, because we cannot
  // load them directly from a JAR.
  def init() {
    if (libraryName == null) {
      return
    }

    if (SystemUtils.IS_OS_WINDOWS && !Settings.get.alwaysTryNative) {
      if (SystemUtils.IS_OS_WINDOWS_XP) {
        OpenComputers.log.warn("Sorry, but Windows XP isn't supported. I'm afraid you'll have to use a newer Windows. I very much recommend upgrading your Windows, anyway, since Microsoft has stopped supporting Windows XP in April 2014.")
        return
      }

      if (SystemUtils.IS_OS_WINDOWS_2003) {
        OpenComputers.log.warn("Sorry, but Windows Server 2003 isn't supported. I'm afraid you'll have to use a newer Windows.")
        return
      }
    }

    val libraryUrl = classOf[Machine].getResource(s"/assets/${Settings.resourceDomain}/lib/$version/$libraryName")
    if (libraryUrl == null) {
      OpenComputers.log.warn(s"Native library with name '$version/$libraryName' not found.")
      return
    }

    val tmpLibName = s"OpenComputersMod-${OpenComputers.Version}-$version-$libraryName"
    val tmpBasePath = if (Settings.get.nativeInTmpDir) {
      val path = System.getProperty("java.io.tmpdir")
      if (path == null) ""
      else if (path.endsWith("/") || path.endsWith("\\")) path
      else path + "/"
    }
    else "./"
    val tmpLibFile = new File(tmpBasePath + tmpLibName)

    // Clean up old library files when not in tmp dir.
    if (!Settings.get.nativeInTmpDir) {
      val libDir = new File(tmpBasePath)
      if (libDir.isDirectory) {
        for (file <- libDir.listFiles(new PatternFilenameFilter("^" + Pattern.quote("OpenComputersMod-") + ".*" + Pattern.quote("-" + libraryName) + "$"))) {
          if (file.compareTo(tmpLibFile) != 0) {
            file.delete()
          }
        }
      }
    }

    // If the file, already exists, make sure it's the same we need, if it's
    // not disable use of the natives.
    if (tmpLibFile.exists()) {
      var matching = true
      try {
        val inCurrent = libraryUrl.openStream()
        val inExisting = new FileInputStream(tmpLibFile)
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
          tmpLibFile.delete()
        }
        catch {
          case t: Throwable => // Ignore.
        }
        if (tmpLibFile.exists()) {
          OpenComputers.log.warn(s"Could not update native library '${tmpLibFile.getName}'!")
        }
      }
    }

    // Copy the file contents to the temporary file.
    try {
      val in = Channels.newChannel(libraryUrl.openStream())
      try {
        val out = new FileOutputStream(tmpLibFile).getChannel
        try {
          out.transferFrom(in, 0, Long.MaxValue)
          tmpLibFile.deleteOnExit()
          // Set file permissions more liberally for multi-user+instance servers.
          tmpLibFile.setReadable(true, false)
          tmpLibFile.setWritable(true, false)
          tmpLibFile.setExecutable(true, false)
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
    currentLib = tmpLibFile.getAbsolutePath
    try {
      LuaStateFactory.synchronized {
        prepareLoad(currentLib)
        create().close()
      }
      OpenComputers.log.info(s"Found a compatible native library: '${tmpLibFile.getName}'.")
      haveNativeLibrary = true
    }
    catch {
      case t: Throwable =>
        if (Settings.get.logFullLibLoadErrors) {
          OpenComputers.log.trace(s"Could not load native library '${tmpLibFile.getName}'.", t)
        }
        else {
          OpenComputers.log.trace(s"Could not load native library '${tmpLibFile.getName}'.")
        }
        tmpLibFile.delete()
    }
  }

  init()

  if (!haveNativeLibrary) {
    OpenComputers.log.warn("Unsupported platform, you won't be able to host games with persistent computers.")
  }

  // ----------------------------------------------------------------------- //
  // Factory
  // ----------------------------------------------------------------------- //

  def createState(): Option[jnlua.LuaState] = {
    if (!haveNativeLibrary) return None

    try {
      val state = LuaStateFactory.synchronized {
        prepareLoad(currentLib)
        if (Settings.get.limitMemory) create(Some(Int.MaxValue))
        else create()
      }
      try {
        // Load all libraries.
        openLibs(state)

        if (!Settings.get.disableLocaleChanging) {
          state.openLib(jnlua.LuaState.Library.OS)
          state.getField(-1, "setlocale")
          state.pushString("C")
          state.call(1, 0)
          state.pop(1)
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
          OpenComputers.log.warn("Failed creating Lua state.", t)
          state.close()
      }
    }
    catch {
      case _: UnsatisfiedLinkError =>
        OpenComputers.log.error("Failed loading the native libraries.")
      case t: Throwable =>
        OpenComputers.log.warn("Failed creating Lua state.", t)
    }
    None
  }

  // Inspired by org.apache.commons.lang3.SystemUtils
  object Architecture {
    val OS_ARCH = try System.getProperty("os.arch") catch {
      case ex: SecurityException => null
    }

    val IS_OS_ARM = isOSArchMatch("arm")

    val IS_OS_A64 = isOSArchMatch("aarch64")

    val IS_OS_X86 = isOSArchMatch("x86") || isOSArchMatch("i386")

    val IS_OS_X64 = isOSArchMatch("x86_64") || isOSArchMatch("amd64")

    private def isOSArchMatch(archPrefix: String): Boolean = OS_ARCH != null && OS_ARCH.startsWith(archPrefix)
  }

}
