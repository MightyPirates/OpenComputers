package li.cil.oc.server.component

import li.cil.oc.common.component
import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.Settings

class UpgradeCapacitor extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferCapacitorUpgrade).
    create()
}
