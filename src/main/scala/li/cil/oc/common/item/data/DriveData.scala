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
  var lockInfo: String = ""

  def isLocked: Boolean = {
    lockInfo != null && !lockInfo.isEmpty
  }

  private final val UnmanagedTag = Settings.namespace + "unmanaged"
  private val LockTag = Settings.namespace + "lock"

  override def load(nbt: NBTTagCompound) {
    isUnmanaged = nbt.getBoolean(UnmanagedTag)
    lockInfo = if (nbt.hasKey(LockTag)) {
      nbt.getString(LockTag)
    } else ""
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setBoolean(UnmanagedTag, isUnmanaged)
    nbt.setString(LockTag, lockInfo)
  }
}
