package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.api.power.Provider
import net.minecraft.nbt.NBTTagCompound

class PowerDistributor extends Rotatable with Provider {

  //MAXENERGY = 2000.0.toDouble

//  override val name = "powerdistributor"

//  override val visibility = Visibility.Network

  override def updateEntity() {
    super.updateEntity()
    update()
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super[Rotatable].readFromNBT(nbt)
//    super.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super[Rotatable].writeToNBT(nbt)
//    super.save(nbt)
  }






  def receive(message: Message) = null

  def update() {}

  def save(nbt: NBTTagCompound) {}

  def load(nbt: NBTTagCompound) {}

  def address() = null

  def host() = null

  def network() = null

  def reachability() = Visibility.None
}
