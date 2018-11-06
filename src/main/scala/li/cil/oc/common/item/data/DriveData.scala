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

  private val UnmanagedKey = Settings.namespace + "unmanaged"
  private val LockKey = Settings.namespace + "lock"

  override def load(nbt: NBTTagCompound) {
    isUnmanaged = nbt.getBoolean(UnmanagedKey)
    lockInfo = if (nbt.hasKey(LockKey)) {
      nbt.getString(LockKey)
    } else ""
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setBoolean(UnmanagedKey, isUnmanaged)
    nbt.setString(LockKey, lockInfo)
  }
}
