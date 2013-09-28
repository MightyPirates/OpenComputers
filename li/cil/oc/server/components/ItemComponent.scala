package li.cil.oc.server.components

import li.cil.oc.api.{Visibility, INetworkNode}
import net.minecraft.nbt.NBTTagCompound

abstract class ItemComponent(val nbt: NBTTagCompound) extends INetworkNode {
  address = nbt.getInteger("address")

  override def visibility = Visibility.Neighbors

  override def address_=(value: Int) = {
    super.address_=(value)
    nbt.setInteger("address", address)
  }
}
