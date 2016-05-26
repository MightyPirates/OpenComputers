package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

class Redstone extends traits.Environment with traits.BundledRedstoneAware with traits.Tickable {
  val instance =
    if (BundledRedstone.isAvailable)
      new component.Redstone.Bundled(this)
    else
      new component.Redstone.Vanilla(this)
  instance.wakeNeighborsOnly = false
  val node = instance.node
  val dummyNode = if (node != null) {
    node.setVisibility(Visibility.Network)
    _isOutputEnabled = true
    api.Network.newNode(this, Visibility.None).create()
  }
  else null

  // ----------------------------------------------------------------------- //

  private final val RedstoneTag = Settings.namespace + "redstone"

  override def updateEntity(): Unit = {
    super[Environment].updateEntity()
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    instance.load(nbt.getCompoundTag(RedstoneTag))
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag(RedstoneTag, instance.save)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(side: EnumFacing, oldMaxValue: Int, newMaxValue: Int) {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    if (node != null && node.network != null) {
      node.connect(dummyNode)
      dummyNode.sendToNeighbors("redstone.changed", side, Int.box(oldMaxValue), Int.box(newMaxValue))
    }
  }
}
