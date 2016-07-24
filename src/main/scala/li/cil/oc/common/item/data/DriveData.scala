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

  private final val UnmanagedTag = Settings.namespace + "unmanaged"

  override def load(nbt: NBTTagCompound) {
    isUnmanaged = nbt.getBoolean(UnmanagedTag)
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setBoolean(UnmanagedTag, isUnmanaged)
  }
}
