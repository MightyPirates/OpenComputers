package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility

class UpgradeAngel extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("angel").
    create()

  override val canUpdate = false
}
