package li.cil.oc.server.machine.luac

import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api.machine._
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.server.component.DebugCard.AccessContext
import li.cil.oc.server.machine.StringBufferValue
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import net.minecraft.nbt.NBTTagCompound

class StringBufferAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  override def initialize(): Unit = {
    lua.newTable()
    lua.pushScalaFunction(lua => {
      def size = lua.checkInteger(1)
      if (size < 0 || size > 2 * 1024 * 1024) {
        throw new IllegalArgumentException("size is out of range")
      }

      if (Settings.get.limitMemory) {
        val deltaMem = (size * owner.ramScale).toInt
        // Should we do GC here, so memory may be more spacious?
        if (lua.getFreeMemory < deltaMem) {
          throw new Exception("not enough memory")
        }
        // Reduce maximum memory so lua code will share it..
        lua.setTotalMemory(lua.getTotalMemory - deltaMem)
        owner.unmanagedMemory += size
      }

      lua.pushJavaObject(new StringBufferValue(size).asInstanceOf[Value])
      1
    })
    lua.setField(-2, "new")

    lua.setGlobal("strbuf")
  }
}
