package li.cil.oc.common.item.data

import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DriveData extends ItemData(null) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var isUnmanaged = false

  override def load(nbt: NBTTagCompound) {
    isUnmanaged = nbt.getBoolean(Settings.namespace + "unmanaged")
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setBoolean(Settings.namespace + "unmanaged", isUnmanaged)
  }
}
