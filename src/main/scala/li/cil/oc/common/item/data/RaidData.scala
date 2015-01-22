package li.cil.oc.common.item.data

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class RaidData extends ItemData {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var disks = Array.empty[ItemStack]

  var filesystem = new NBTTagCompound()

  var label: Option[String] = None

  override def load(nbt: NBTTagCompound): Unit = {
    disks = nbt.getTagList(Settings.namespace + "disks", NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(ItemUtils.loadStack)
    filesystem = nbt.getCompoundTag(Settings.namespace + "filesystem")
    if (nbt.hasKey(Settings.namespace + "label")) {
      label = Option(nbt.getString(Settings.namespace + "label"))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    nbt.setNewTagList(Settings.namespace + "disks", disks.toIterable)
    nbt.setTag(Settings.namespace + "filesystem", filesystem)
    label.foreach(nbt.setString(Settings.namespace + "label", _))
  }

  def createItemStack() = {
    val stack = api.Items.get("raid").createItemStack(1)
    save(stack)
    stack
  }
}
