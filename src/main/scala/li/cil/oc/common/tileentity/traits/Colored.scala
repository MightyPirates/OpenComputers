package li.cil.oc.common.tileentity.traits

import net.minecraft.nbt.NBTTagCompound
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings

trait Colored extends TileEntity {
  var color = 0

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (nbt.hasKey(Settings.namespace + "renderColor")) {
      color = nbt.getInteger(Settings.namespace + "renderColor")
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setInteger(Settings.namespace + "renderColor", color)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    color = nbt.getInteger("renderColor")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("renderColor", color)
  }
}
