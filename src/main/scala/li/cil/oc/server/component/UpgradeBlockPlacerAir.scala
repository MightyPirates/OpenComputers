package li.cil.oc.server.component


import net.minecraft.tileentity.TileEntity
import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility

class UpgradeBlockPlacerAir(val owner: TileEntity) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("airplacer").
    create()

  // ----------------------------------------------------------------------- //

  override val canUpdate = false

}
