package li.cil.oc.common.tileentity

import li.cil.oc.api.network

trait Environment extends network.Environment {
  def onMessage(message: network.Message) {}

  def onConnect(node: network.Node) {}

  def onDisconnect(node: network.Node) {}
}
