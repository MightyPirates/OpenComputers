package li.cil.oc.common.item.data

import li.cil.oc.{Constants, Settings}
import li.cil.oc.server.component.DebugCard.AccessContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DebugCardData extends ItemData(Constants.ItemName.DebugCard) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var access: Option[AccessContext] = None

  override def load(nbt: NBTTagCompound): Unit = {
    access = AccessContext.load(dataTag(nbt))
  }

  override def save(nbt: NBTTagCompound): Unit = {
    val tag = dataTag(nbt)
    AccessContext.remove(tag)
    access.foreach(_.save(tag))
  }

  private def dataTag(nbt: NBTTagCompound) = {
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setTag(Settings.namespace + "data", new NBTTagCompound())
    }
    nbt.getCompoundTag(Settings.namespace + "data")
  }
}
