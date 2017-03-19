package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class RaidData extends ItemData(Constants.BlockName.Raid) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var disks = Array.empty[ItemStack]

  var filesystem = new NBTTagCompound()

  var label: Option[String] = None

  private final val DisksTag = Constants.namespace + "disks"
  private final val FileSystemTag = Constants.namespace + "filesystem"
  private final val LabelTag = Constants.namespace + "label"

  override def load(nbt: NBTTagCompound): Unit = {
    disks = nbt.getTagList(DisksTag, NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(new ItemStack(_))
    filesystem = nbt.getCompoundTag(FileSystemTag)
    if (nbt.hasKey(LabelTag)) {
      label = Option(nbt.getString(LabelTag))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    nbt.setNewTagList(DisksTag, disks.toIterable)
    nbt.setTag(FileSystemTag, filesystem)
    label.foreach(nbt.setString(LabelTag, _))
  }
}
