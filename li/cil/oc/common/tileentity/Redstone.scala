package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.server.component
import li.cil.oc.util.mods.BundledRedstone
import net.minecraftforge.common.ForgeDirection

class Redstone extends Environment with BundledRedstoneAware {
  val instance = if (BundledRedstone.isAvailable) new component.BundledRedstone(this) else new component.Redstone(this)
  val node = instance.node
  if (node != null) node.setVisibility(Visibility.Network)

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      isOutputEnabled = true
      updateRedstoneInput()
    }
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    node.sendToReachable("computer.signal", "redstone_changed", Int.box(side.ordinal()))
  }
}
