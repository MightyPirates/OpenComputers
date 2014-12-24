package li.cil.oc.server.component.traits

import li.cil.oc.api.network.Node

trait NetworkAware {
  def node: Node
}
