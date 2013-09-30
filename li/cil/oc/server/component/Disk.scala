package li.cil.oc.server.component

import li.cil.oc.api.network.Node
import net.minecraft.nbt.NBTTagCompound

class Disk(val nbt: NBTTagCompound) extends Node {
  override def name = "disk"

  def close() {}
}
