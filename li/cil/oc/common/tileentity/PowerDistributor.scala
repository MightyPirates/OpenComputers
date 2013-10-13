package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import scala.collection.mutable
import net.minecraft.nbt.NBTTagCompound

class PowerDistributor  extends Rotatable with Provider {


  //MAXENERGY = 2000.0.toDouble

  override val name = "powerdistributor"

  override val visibility = Visibility.Network

  override def updateEntity(){
    super.updateEntity()
    update()
  }

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    save(nbt)
  }
}
