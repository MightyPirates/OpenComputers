package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component

class UpgradeBattery(val tier: Int) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferCapacitorUpgrades(tier)).
    create()
}
