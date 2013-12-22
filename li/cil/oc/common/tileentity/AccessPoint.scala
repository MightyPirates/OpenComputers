package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.Visibility

class AccessPoint extends Environment {
  val node = api.Network.newNode(this, Visibility.Network).create()
}
