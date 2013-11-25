package li.cil.oc.server.component

import li.cil.oc.api.network.Visibility
import li.cil.oc.{Settings, api}

class PowerSupply extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferPowerSupply).
    create()

  override def update() {
    super.update()
    node.changeBuffer(-Settings.get.powerSupplyCost)
  }
}
