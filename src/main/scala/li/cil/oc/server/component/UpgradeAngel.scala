package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab

// Note-to-self: this has a component to allow the robot telling it has the
// upgrade.
class UpgradeAngel extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("angel").
    create()
}
