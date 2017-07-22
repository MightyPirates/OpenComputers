package li.cil.oc.common.item.data

import li.cil.oc.Settings
import li.cil.oc.api.network.Visibility
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

// Generic one for items that are used as components; gets the items node info.
class NodeData extends ItemData(null) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var address: Option[String] = None
  var buffer: Option[Double] = None
  var visibility: Option[Visibility] = None

  override def load(nbt: NBTTagCompound): Unit = {
    val nodeNbt = nbt.getCompoundTag(Settings.namespace + "data").getCompoundTag("node")
    if (nodeNbt.hasKey("address")) {
      address = Option(nodeNbt.getString("address"))
    }
    if (nodeNbt.hasKey("buffer")) {
      buffer = Option(nodeNbt.getDouble("buffer"))
    }
    if (nodeNbt.hasKey("visibility")) {
      visibility = Option(Visibility.values()(nodeNbt.getInteger("visibility")))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setTag(Settings.namespace + "data", new NBTTagCompound())
    }
    val dataNbt = nbt.getCompoundTag(Settings.namespace + "data")
    if (!dataNbt.hasKey("node")) {
      dataNbt.setTag("node", new NBTTagCompound())
    }
    val nodeNbt = dataNbt.getCompoundTag("node")
    address.foreach(nodeNbt.setString("address", _))
    buffer.foreach(nodeNbt.setDouble("buffer", _))
    visibility.map(_.ordinal()).foreach(nodeNbt.setInteger("visibility", _))
  }
}
