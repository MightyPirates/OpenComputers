package li.cil.oc.server.component

import li.cil.oc.api.network.{Visibility, Node}
import net.minecraft.nbt.NBTTagCompound

abstract class ItemComponent(val nbt: NBTTagCompound) extends Node {
  address = nbt.getInteger("address")

  override def visibility = Visibility.Neighbors

  override def address_=(value: Int) = {
    super.address_=(value)
    nbt.setInteger("address", address)
  }
}
