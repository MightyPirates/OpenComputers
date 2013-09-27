package li.cil.oc.server.components

import li.cil.oc.api.INetworkNode
import net.minecraft.nbt.NBTTagCompound

class Disk(val nbt: NBTTagCompound) extends INetworkNode {
  override def name = "disk"

  def close() {}
}
