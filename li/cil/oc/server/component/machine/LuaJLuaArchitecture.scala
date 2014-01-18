package li.cil.oc.server.component.machine

import li.cil.oc.server.component.Machine
import net.minecraft.nbt.NBTTagCompound
import org.luaj.vm2.lib.jse.JsePlatform

class LuaJLuaArchitecture(machine: Machine) extends LuaArchitecture(machine) {
  protected def createState() = {
    lua = JsePlatform.standardGlobals()
    true
  }

  def recomputeMemory() {}

  def load(nbt: NBTTagCompound) {}

  def save(nbt: NBTTagCompound) {}
}
