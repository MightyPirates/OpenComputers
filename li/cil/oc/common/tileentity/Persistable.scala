package li.cil.oc.common.tileentity

import li.cil.oc.api
import net.minecraft.nbt.NBTTagCompound

trait Persistable extends api.Persistable {
  def save(nbt: NBTTagCompound) {}

  def load(nbt: NBTTagCompound) {}
}
