package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.nbt.NBTTagCompound

class Transposer extends traits.Environment {
  val transposer = new component.Transposer.Block(this)

  def node = transposer.node

  // Used on client side to check whether to render activity indicators.
  var lastOperation = 0L

  override def canUpdate = false

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    transposer.load(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    transposer.save(nbt)
  }
}
