package li.cil.oc.common.item.data

import li.cil.oc.server.component.DebugCard.AccessContext
import li.cil.oc.Constants
import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DebugCardData extends ItemData(Constants.ItemName.DebugCard) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var access: Option[AccessContext] = None

  private final val DataTag = Constants.namespace + "data"

  override def load(nbt: NBTTagCompound): Unit = {
    access = AccessContext.load(dataTag(nbt))
  }

  override def save(nbt: NBTTagCompound): Unit = {
    val tag = dataTag(nbt)
    AccessContext.remove(tag)
    access.foreach(_.save(tag))
  }

  private def dataTag(nbt: NBTTagCompound) = {
    if (!nbt.hasKey(DataTag)) {
      nbt.setTag(DataTag, new NBTTagCompound())
    }
    nbt.getCompoundTag(DataTag)
  }
}
