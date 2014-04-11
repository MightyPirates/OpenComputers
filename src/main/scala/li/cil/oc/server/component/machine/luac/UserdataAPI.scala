package li.cil.oc.server.component.machine.luac

import java.io.{DataInputStream, ByteArrayInputStream, DataOutputStream, ByteArrayOutputStream}
import li.cil.oc.api.machine.Value
import li.cil.oc.api.Persistable
import li.cil.oc.server.component.machine.NativeLuaArchitecture
import li.cil.oc.server.network.{Callbacks, Arguments}
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
    })
    lua.setField(-2, "load")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      value.apply(machine, new Arguments(lua.toSimpleJavaObjects(2))) match {
        case results: Array[_] =>
          results.foreach(result => lua.pushValue(result))
          results.length
        case _ =>
          0
      }
    })
    lua.setField(-2, "apply")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      value.unapply(machine, new Arguments(lua.toSimpleJavaObjects(2)))
      0
    })
    lua.setField(-2, "unapply")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      value.call(machine, new Arguments(lua.toSimpleJavaObjects(2))) match {
        case results: Array[_] =>
          results.foreach(result => lua.pushValue(result))
          results.length
        case _ =>
          0
      }
    })
    lua.setField(-2, "call")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      value.dispose(machine)
      0
    })
    lua.setField(-2, "dispose")

    lua.pushScalaFunction(lua => {
      val value = lua.toJavaObjectRaw(1).asInstanceOf[Value]
      lua.pushList(Callbacks(value).keysIterator.zipWithIndex)
      0
    })
    lua.setField(-2, "callbacks")

    lua.setGlobal("userdata")

  }
}
