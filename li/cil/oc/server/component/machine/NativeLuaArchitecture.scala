package li.cil.oc.server.component.machine

import li.cil.oc.server.component.Machine
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.OpenComputers
import scala.collection.mutable
import li.cil.oc.util.LuaStateFactory
import com.naef.jnlua

class NativeLuaArchitecture(machine: Machine) extends LuaArchitecture(machine) {
  override var lua: jnlua.LuaState = _

  def recomputeMemory() = Option(lua) match {
    case Some(l) =>
      l.setTotalMemory(Int.MaxValue)
      l.gc(jnlua.LuaState.GcAction.COLLECT, 0)
      if (kernelMemory > 0) {
        l.setTotalMemory(kernelMemory + math.ceil(machine.owner.installedMemory * ramScale).toInt)
      }
    case _ =>
  }

  // ----------------------------------------------------------------------- //

  override def init(): Boolean = {
    if (super.init()) {
      initPerms()
      true
    }
    else false
  }

  override def close() {
    if (lua != null) {
      lua.setTotalMemory(Integer.MAX_VALUE)
      lua.close()
    }
    lua = null
    super.close()
  }

  protected def createState() = {
    // Creates a new state with all base libraries and the persistence library
    // loaded into it. This means the state has much more power than it
    // rightfully should have, so we sandbox it a bit in the following.
    LuaStateFactory.createState() match {
      case Some(value) =>
        lua = value
        true
      case _ =>
        lua = null
        machine.message = Some("native libraries not available")
        false
    }
  }

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
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
      if (state.contains(Machine.State.SynchronizedCall) || state.contains(Machine.State.SynchronizedReturn)) {
        unpersist(nbt.getByteArray("stack"))
        if (!(if (state.contains(Machine.State.SynchronizedCall)) lua.isFunction(2) else lua.isTable(2))) {
          // Same as with the above, should not really happen normally, but
          // could for the same reasons.
          throw new IllegalArgumentException("Invalid stack.")
        }
      }

      kernelMemory = (nbt.getInteger("kernelMemory") * ramScale).toInt
    } catch {
      case e: jnlua.LuaRuntimeException =>
        OpenComputers.log.warning("Could not unpersist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        state.push(Machine.State.Stopping)
    }

    // Limit memory again.
    recomputeMemory()
  }

  def save(nbt: NBTTagCompound) {
    // Unlimit memory while persisting.
    lua.setTotalMemory(Integer.MAX_VALUE)

    try {
      // Try persisting Lua, because that's what all of the rest depends on.
      // Save the kernel state (which is always at stack index one).
      assert(lua.isThread(1))
      nbt.setByteArray("kernel", persist(1))
      // While in a driver call we have one object on the global stack: either
      // the function to call the driver with, or the result of the call.
      if (state.contains(Machine.State.SynchronizedCall) || state.contains(Machine.State.SynchronizedReturn)) {
        assert(if (state.contains(Machine.State.SynchronizedCall)) lua.isFunction(2) else lua.isTable(2))
        nbt.setByteArray("stack", persist(2))
      }

      nbt.setInteger("kernelMemory", math.ceil(kernelMemory / ramScale).toInt)
    } catch {
      case e: jnlua.LuaRuntimeException =>
        OpenComputers.log.warning("Could not persist computer.\n" + e.toString + "\tat " + e.getLuaStackTrace.mkString("\n\tat "))
        nbt.removeTag("state")
    }

    // Limit memory again.
    recomputeMemory()
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
    lua.setField(jnlua.LuaState.REGISTRYINDEX, "uperms") /* ... perms */
    lua.setField(jnlua.LuaState.REGISTRYINDEX, "perms") /* ... */
  }

  private def persist(index: Int): Array[Byte] = {
    lua.getGlobal("eris") /* ... eris */
    lua.getField(-1, "persist") /* ... eris persist */
    if (lua.isFunction(-1)) {
      lua.getField(jnlua.LuaState.REGISTRYINDEX, "perms") /* ... eris persist perms */
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
      lua.getField(jnlua.LuaState.REGISTRYINDEX, "uperms") /* ... eris persist uperms */
      lua.pushByteArray(value) // ... eris unpersist uperms str
      lua.call(2, 1) // ... eris obj
      lua.insert(-2) // ... obj eris
      lua.pop(1)
      return true
    } // ... :(
    lua.pop(1)
    false
  }
}
