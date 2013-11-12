package li.cil.oc.common.tileentity

import li.cil.oc.api.{Network, network}
import net.minecraft.nbt.NBTTagCompound

abstract class Environment extends net.minecraft.tileentity.TileEntity with TileEntity with network.Environment {

  def world = worldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (node != null && node.network == null) {
      Network.joinOrCreateNetwork(worldObj, xCoord, yCoord, zCoord)
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (node != null) node.remove()
  }

  override def invalidate() {
    super.invalidate()
    if (node != null) node.remove()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (node != null) node.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (node != null) node.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  def onMessage(message: network.Message) {}

  def onConnect(node: network.Node) {}

  def onDisconnect(node: network.Node) {}
}
