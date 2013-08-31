package li.cil.oc.common.computer

import scala.collection.Seq
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.server.computer.IComputerContext
import com.naef.jnlua.LuaState

trait IInternalComputerContext extends IComputerContext {
  def luaState: LuaState

  def start(): Boolean

  def stop(): Unit

  def update()

  def readFromNBT(nbt: NBTTagCompound)

  def writeToNBT(nbt: NBTTagCompound)
}