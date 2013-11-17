package li.cil.oc.server.component

import li.cil.oc.api.network.Visibility
import li.cil.oc.{Config, api}

class PowerSupply extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Config.bufferPowerSupply).
    create()

  override def update() {
    super.update()
    node.changeBuffer(-Config.powerSupplyCost)
  }
}
