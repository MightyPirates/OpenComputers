package li.cil.oc.server.component.machine.luac

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util.logging.Level

import li.cil.oc.OpenComputers
import li.cil.oc.api.Persistable
import li.cil.oc.api.machine.Value
import li.cil.oc.server.component.machine.NativeLuaArchitecture
import li.cil.oc.server.network.{ArgumentsImpl, Callbacks}
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}

class UserdataAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  def initialize() {
    lua.newTable()

    lua.pushScalaFunction(lua => {
      val nbt = new NBTTagCompound()
      val persistable = lua.toJavaObjectRaw(1).asInstanceOf[Persistable]
      lua.pushString(persistable.getClass.getName)
      persistable.save(nbt)
      val baos = new ByteArrayOutputStream()
      val dos = new DataOutputStream(baos)
      CompressedStreamTools.write(nbt, dos)
      lua.pushByteArray(baos.toByteArray)
      2
    })
    lua.setField(-2, "save")

    lua.pushScalaFunction(lua => {
      try {
        val className = lua.toString(1)
        val clazz = Class.forName(className)
        val persistable = clazz.newInstance.asInstanceOf[Persistable]
        val data = lua.toByteArray(2)
        val bais = new ByteArrayInputStream(data)
        val dis = new DataInputStream(bais)
        val nbt = CompressedStreamTools.read(dis)
        persistable.load(nbt)
        lua.pushJavaObjectRaw(persistable)
        1
      }
      catch {
        case t: Throwable =>
          OpenComputers.log.log(Level.WARNING, "Error in userdata load function.", t)
          throw t
      }
    })
    lua.setField(-2, "load")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      val args = lua.toSimpleJavaObjects(2)
      owner.invoke(() => Array(value.apply(machine, new ArgumentsImpl(args))))
    })
    lua.setField(-2, "apply")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      val args = lua.toSimpleJavaObjects(2)
      owner.invoke(() => {
        value.unapply(machine, new ArgumentsImpl(args))
        null
      })
    })
    lua.setField(-2, "unapply")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      val args = lua.toSimpleJavaObjects(2)
      owner.invoke(() => value.call(machine, new ArgumentsImpl(args)))
    })
    lua.setField(-2, "call")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      try value.dispose(machine) catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Error in dispose method of userdata of type " + value.getClass.getName, t)
      }
      0
    })
    lua.setField(-2, "dispose")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      lua.pushValue(Callbacks(value).map(entry => entry._1 -> entry._2.direct))
      1
    })
    lua.setField(-2, "methods")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      val method = lua.checkString(2)
      val args = lua.toSimpleJavaObjects(3)
      owner.invoke(() => owner.machine.invoke(value, method, args.toArray))
    })
    lua.setField(-2, "invoke")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      val method = lua.checkString(2)
      owner.documentation(() => owner.machine.documentation(value, method))
    })
    lua.setField(-2, "doc")

    lua.setGlobal("userdata")
  }
}
