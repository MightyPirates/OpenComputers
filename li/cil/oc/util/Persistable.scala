package li.cil.oc.util

import li.cil.oc.api
import net.minecraft.nbt.NBTTagCompound

trait Persistable extends api.Persistable {
  def save(nbt: NBTTagCompound) {}

  def load(nbt: NBTTagCompound) {}
}
