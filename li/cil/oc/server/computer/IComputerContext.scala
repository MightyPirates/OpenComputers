package li.cil.oc.server.computer

import com.naef.jnlua.LuaState

import net.minecraft.nbt.NBTTagCompound

trait IComputerContext {
  def signal(pid: Int, name: String, args: Any*)
}