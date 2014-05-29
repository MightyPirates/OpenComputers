package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component

// Note-to-self: this has a component to allow the robot telling it has the
// upgrade.
class UpgradeAngel extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("angel").
    create()
}
