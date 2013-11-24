package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.{Config, api}

class Capacitor extends Environment {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Config.bufferCapacitor).
    create()
}
