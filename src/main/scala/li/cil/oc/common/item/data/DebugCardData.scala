package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.server.component.DebugCard.AccessContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

class DebugCardData extends ItemData(Constants.ItemName.DebugCard) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var access: Option[AccessContext] = None

  private final val DataTag = Settings.namespace + "data"

  override def loadData(nbt: CompoundNBT): Unit = {
    access = AccessContext.loadData(dataTag(nbt))
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    val tag = dataTag(nbt)
    AccessContext.remove(tag)
    access.foreach(_.saveData(tag))
  }

  private def dataTag(nbt: CompoundNBT) = {
    if (!nbt.contains(DataTag)) {
      nbt.put(DataTag, new CompoundNBT())
    }
    nbt.getCompound(DataTag)
  }
}
