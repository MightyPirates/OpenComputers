package li.cil.oc.server.machine.luac

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedLuaState._
import li.cil.repack.com.naef.jnlua.LuaState
import net.minecraft.nbt.CompoundNBT

import scala.collection.mutable

class PersistenceAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  private var persistKey = "__persist" + UUID.randomUUID().toString.replaceAll("-", "")

  override def initialize() {
    // Will be replaced by old value in load.
    lua.pushScalaFunction(lua => {
      lua.pushString(persistKey)
      1
    })
    lua.setGlobal("persistKey")

    if (Settings.get.allowPersistence) {
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
  }

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
    if (nbt.contains("persistKey")) {
      persistKey = nbt.getString("persistKey")
    }
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)
    nbt.putString("persistKey", persistKey)
  }

  def configure() {
    lua.getGlobal("eris")

    lua.getField(-1, "settings")
    lua.pushString("spkey")
    lua.pushString(persistKey)
    lua.call(2, 0)

    lua.getField(-1, "settings")
    lua.pushString("path")
    lua.pushBoolean(Settings.get.debugPersistence)
    lua.call(2, 0)

    lua.pop(1)
  }

  def persist(index: Int): Array[Byte] = {
    if (Settings.get.allowPersistence) {
      configure()
      try {
        lua.gc(LuaState.GcAction.STOP, 0)
        lua.getGlobal("eris") // ... eris
        lua.getField(-1, "persist") // ... eris persist
        if (lua.isFunction(-1)) {
          lua.getField(LuaState.REGISTRYINDEX, "perms") // ... eris persist perms
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
      }
      finally {
        lua.gc(LuaState.GcAction.RESTART, 0)
      }
    }
    Array[Byte]()
  }

  def unpersist(value: Array[Byte]): Boolean = {
    if (Settings.get.allowPersistence) {
      configure()
      try {
        lua.gc(LuaState.GcAction.STOP, 0)
        lua.getGlobal("eris") // ... eris
        lua.getField(-1, "unpersist") // ... eris unpersist
        if (lua.isFunction(-1)) {
          lua.getField(LuaState.REGISTRYINDEX, "uperms") // ... eris persist uperms
          lua.pushByteArray(value) // ... eris unpersist uperms str
          lua.call(2, 1) // ... eris obj
          lua.insert(-2) // ... obj eris
          lua.pop(1)
          return true
        } // ... :(
        lua.pop(1)
      }
      finally {
        lua.gc(LuaState.GcAction.RESTART, 0)
      }
    }
    false
  }
}
