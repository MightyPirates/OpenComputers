package li.cil.oc.common.item.data

import li.cil.oc.Settings
import li.cil.oc.api.network.Visibility
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

// Generic one for items that are used as components; gets the items node info.
class NodeData extends ItemData(null) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var address: Option[String] = None
  var buffer: Option[Double] = None
  var visibility: Option[Visibility] = None

  private final val DataTag = Settings.namespace + "data"

  override def loadData(nbt: CompoundNBT): Unit = {
    val nodeNbt = nbt.getCompound(DataTag).getCompound(NodeData.NodeTag)
    if (nodeNbt.contains(NodeData.AddressTag)) {
      address = Option(nodeNbt.getString(NodeData.AddressTag))
    }
    if (nodeNbt.contains(NodeData.BufferTag)) {
      buffer = Option(nodeNbt.getDouble(NodeData.BufferTag))
    }
    if (nodeNbt.contains(NodeData.VisibilityTag)) {
      visibility = Option(Visibility.values()(nodeNbt.getInt(NodeData.VisibilityTag)))
    }
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    if (!nbt.contains(DataTag)) {
      nbt.put(DataTag, new CompoundNBT())
    }
    val dataNbt = nbt.getCompound(DataTag)
    if (!dataNbt.contains(NodeData.NodeTag)) {
      dataNbt.put(NodeData.NodeTag, new CompoundNBT())
    }
    val nodeNbt = dataNbt.getCompound(NodeData.NodeTag)
    address.foreach(nodeNbt.putString(NodeData.AddressTag, _))
    buffer.foreach(nodeNbt.putDouble(NodeData.BufferTag, _))
    visibility.map(_.ordinal()).foreach(nodeNbt.putInt(NodeData.VisibilityTag, _))
  }
}

object NodeData {
  final val NodeTag = "node"
  final val AddressTag = "address"
  final val BufferTag = "buffer"
  final val VisibilityTag = "visibility"
}