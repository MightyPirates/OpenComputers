package li.cil.oc.server.machine

import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.server.machine.luac.NativeLuaArchitecture
import net.minecraft.nbt.NBTTagCompound


class StringBufferValue() extends AbstractValue {
  /* TODO: fix koefficients */
  final val limitBase = 10000
  final val readCost = 0.1
  final val writeCost = 0.2

  var buffer: Array[Byte] = _
  def this(size: Integer) = {
    this()
    this.buffer = new Array[Byte](size)
  }
  def result(args: Any*): Array[AnyRef] = li.cil.oc.util.ResultWrapper.result(args: _*)

  @Callback(direct = true, limit = limitBase)
  def write(ctx: Context, arg: Arguments): Array[AnyRef] = {
    ctx.consumeCallBudget(writeCost * buffer.length / limitBase)
    val offset = arg.checkDouble(0).toInt
    val value = arg.checkByteArray(1)
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be positive")
    }
    for (i <- offset until math.min(offset + value.length, buffer.length)) {
      buffer(i) = value(i - offset)
    }
    null
  }

  @Callback(direct = true, limit = limitBase)
  def read(ctx: Context,arg: Arguments): Array[AnyRef] = {
    ctx.consumeCallBudget(readCost * buffer.length / limitBase)
    val offset = arg.checkInteger(0)
    var sz = 1
    if (arg.count() >= 2) {
      sz = arg.checkInteger(1)
      if (sz < 0) {
        throw new IllegalArgumentException("size must be positive")
      }
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be positive")
    }
    val res = new Array[Byte](math.max(math.min(sz, buffer.length - offset), 0))
    for (i <- offset until math.min(offset + sz, buffer.length)) {
      res(i - offset) = buffer(i)
    }
    result(res)
  }

  @Callback(direct = true, limit = limitBase * 10)
  def length(ctx: Context, arg: Arguments): Array[AnyRef] = {
    result(buffer.length)
  }

  @Callback(direct = true, limit = limitBase)
  def tostring(ctx: Context, arg: Arguments): Array[AnyRef] = {
    ctx.consumeCallBudget(readCost * buffer.length / limitBase)
    result(buffer)
  }

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    buffer = nbt.getByteArray("buffer")
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    nbt.setByteArray("buffer", buffer)
  }

  override def dispose(context: Context): Unit = {
    if (Settings.get.limitMemory) {
      context match {
        case m: Machine =>
          m.architecture match {
            case arch: NativeLuaArchitecture =>
              val lua = arch.lua

              // Give memory back to lua code
              arch.unmanagedMemory -= buffer.length
              lua.setTotalMemory(lua.getTotalMemory + (buffer.length * arch.ramScale).toInt)
            case _ =>
            // Do nothing here, because LuaJ does not even care about memory
          }

        case _ =>
          // Can this ever happen?
          OpenComputers.log.warn("StringBuffer is disposed in unknown context; this will cause memory leak")
      }
    }
  }
}
