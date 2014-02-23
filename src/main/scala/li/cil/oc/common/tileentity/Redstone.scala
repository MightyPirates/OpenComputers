package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api.network.Visibility
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.mods.BundledRedstone
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Redstone extends Environment with BundledRedstoneAware {
  val instance = if (BundledRedstone.isAvailable) new component.BundledRedstone(this) else new component.Redstone(this)
  val node = instance.node
  if (node != null) {
    node.setVisibility(Visibility.Network)
    _isOutputEnabled = true
  }

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      updateRedstoneInput()
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    instance.load(nbt.getCompoundTag(Settings.namespace + "redstone"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "redstone", instance.save)
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    node.sendToReachable("computer.signal", "redstone_changed", Int.box(side.ordinal()))
  }
}
