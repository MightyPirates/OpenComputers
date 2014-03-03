package li.cil.oc.server.component.machine

import li.cil.oc.api.FileSystem
import li.cil.oc.api.machine.Architecture
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, OpenComputers}
import net.minecraft.nbt.NBTTagCompound

abstract class LuaArchitecture(val machine: Machine) extends Architecture {
  val rom = Option(FileSystem.asManagedEnvironment(FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/rom"), "rom"))

  override def init() = {
    if (machine.node.network != null) {
      rom.foreach(fs => machine.node.connect(fs.node))
    }
    true
  }

  override def onConnect() {
    rom.foreach(fs => machine.node.connect(fs.node))
  }

  override def close() {
    rom.foreach(_.node.remove())
  }

  override def load(nbt: NBTTagCompound) {
    rom.foreach(fs => fs.load(nbt.getCompoundTag("rom")))
  }

  override def save(nbt: NBTTagCompound) {
    rom.foreach(fs => nbt.setNewCompoundTag("rom", fs.save))
  }
}
