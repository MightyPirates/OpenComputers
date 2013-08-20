package li.cil.oc.server.computer

import com.naef.jnlua.LuaState

import net.minecraft.nbt.NBTTagCompound

trait IComputerContext {
  def luaState: LuaState

  def update()

  def readFromNBT(nbt: NBTTagCompound)

  def writeToNBT(nbt: NBTTagCompound)
}