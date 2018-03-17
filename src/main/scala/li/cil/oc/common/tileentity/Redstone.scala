package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound

class Redstone extends traits.Environment with traits.BundledRedstoneAware {
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

  override def canUpdate = isServer

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    instance.load(nbt.getCompoundTag(Settings.namespace + "redstone"))
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "redstone", instance.save)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs) {
    super.onRedstoneInputChanged(args)
    if (node != null && node.network != null) {
      node.connect(dummyNode)
      dummyNode.sendToNeighbors("redstone.changed", args)
    }
  }
}
