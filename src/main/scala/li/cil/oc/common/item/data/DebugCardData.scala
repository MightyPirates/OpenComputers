package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DebugCardData extends ItemData(Constants.ItemName.DebugCard) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var player: Option[String] = None

  private final val DataTag = Settings.namespace + "data"
  private final val PlayerTag = Settings.namespace + "player"

  override def load(nbt: NBTTagCompound) {
    val tag = dataTag(nbt)
    if (tag.hasKey(PlayerTag)) {
      player = Option(tag.getString(PlayerTag))
    }
  }

  override def save(nbt: NBTTagCompound) {
    val tag = dataTag(nbt)
    tag.removeTag(PlayerTag)
    player.foreach(tag.setString(PlayerTag, _))
  }

  private def dataTag(nbt: NBTTagCompound) = {
    if (!nbt.hasKey(DataTag)) {
      nbt.setTag(DataTag, new NBTTagCompound())
    }
    nbt.getCompoundTag(DataTag)
  }
}
