package li.cil.oc.common.item.data

import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.server.fs
import net.minecraft.entity.player.EntityPlayer

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

object DriveData {
  def lock(stack: ItemStack, player: EntityPlayer): Unit = {
    val key = player.getName
    val data = new DriveData(stack)
    if (!data.isLocked) {
      data.lockInfo = key match {
        case name: String if name != null && name.nonEmpty => name
        case _ => "notch" // meaning: "unknown"
      }
      data.save(stack)
    }
  }

  def setUnmanaged(stack: ItemStack, unmanaged: Boolean): Unit = {
    val data = new DriveData(stack)
    if (data.isUnmanaged != unmanaged) {
      fs.FileSystem.removeAddress(stack)
      data.lockInfo = ""
    }
    data.isUnmanaged = unmanaged
    data.save(stack)
  }
}
