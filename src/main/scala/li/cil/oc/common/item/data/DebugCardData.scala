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

  override def load(nbt: NBTTagCompound) {
    val tag = dataTag(nbt)
    if (tag.hasKey(Settings.namespace + "player")) {
      player = Option(tag.getString(Settings.namespace + "player"))
    }
  }

  override def save(nbt: NBTTagCompound) {
    val tag = dataTag(nbt)
    tag.removeTag(Settings.namespace + "player")
    player.foreach(tag.setString(Settings.namespace + "player", _))
  }

  private def dataTag(nbt: NBTTagCompound) = {
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setTag(Settings.namespace + "data", new NBTTagCompound())
    }
    nbt.getCompoundTag(Settings.namespace + "data")
  }
}
