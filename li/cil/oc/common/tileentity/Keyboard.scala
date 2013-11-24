package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, Config}
import net.minecraft.nbt.NBTTagCompound

class Keyboard(isRemote: Boolean) extends Environment with Rotatable {
  def this() = this(false)

  val keyboard = if (isRemote) null else new component.Keyboard(this)

  def node = if (isClient) null else keyboard.node

  override def isClient = keyboard == null

  override def canUpdate = false

  override def validate() {
    super.validate()
    world.scheduleBlockUpdateFromLoad(x, y, z, Blocks.keyboard.parent.blockID, 0, 0)
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      keyboard.load(nbt.getCompoundTag(Config.namespace + "keyboard"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Config.namespace + "keyboard", keyboard.save)
    }
  }
}
