package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.nbt.NBTTagCompound

class Keyboard(isRemote: Boolean) extends Environment with Rotatable {
  def this() = this(false)

  val keyboard = if (isRemote) null else new component.Keyboard(this)

  def node = if (isClient) null else keyboard.node

  override def isClient = keyboard == null

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      keyboard.node.load(nbt)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      keyboard.node.save(nbt)
    }
  }
}
