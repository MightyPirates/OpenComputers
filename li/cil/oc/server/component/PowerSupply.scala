package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.Visibility

class PowerSupply extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector().
    create()

  override def update() {
    super.update()
    node.changeBuffer(1.75)
  }
}
