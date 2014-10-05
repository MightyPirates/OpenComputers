package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab

class UpgradeBattery(val tier: Int) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferCapacitorUpgrades(tier)).
    create()
}
