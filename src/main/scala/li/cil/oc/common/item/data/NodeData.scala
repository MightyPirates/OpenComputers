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

  private final val DataTag = Constants.namespace + "data"

  override def load(nbt: NBTTagCompound): Unit = {
    val nodeNbt = nbt.getCompoundTag(DataTag).getCompoundTag(NodeData.NodeTag)
    if (nodeNbt.hasKey(NodeData.AddressTag)) {
      address = Option(nodeNbt.getString(NodeData.AddressTag))
    }
    if (nodeNbt.hasKey(NodeData.BufferTag)) {
      buffer = Option(nodeNbt.getDouble(NodeData.BufferTag))
    }
    if (nodeNbt.hasKey(NodeData.VisibilityTag)) {
      visibility = Option(Visibility.values()(nodeNbt.getInteger(NodeData.VisibilityTag)))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    if (!nbt.hasKey(DataTag)) {
      nbt.setTag(DataTag, new NBTTagCompound())
    }
    val dataNbt = nbt.getCompoundTag(DataTag)
    if (!dataNbt.hasKey(NodeData.NodeTag)) {
      dataNbt.setTag(NodeData.NodeTag, new NBTTagCompound())
    }
    val nodeNbt = dataNbt.getCompoundTag(NodeData.NodeTag)
    address.foreach(nodeNbt.setString(NodeData.AddressTag, _))
    buffer.foreach(nodeNbt.setDouble(NodeData.BufferTag, _))
    visibility.map(_.ordinal()).foreach(nodeNbt.setInteger(NodeData.VisibilityTag, _))
  }
}

object NodeData {
  final val NodeTag = "node"
  final val AddressTag = "address"
  final val BufferTag = "buffer"
  final val VisibilityTag = "visibility"
}