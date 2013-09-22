package li.cil.oc.server.computer

import li.cil.oc.api.INetwork
import li.cil.oc.api.INetworkMessage
import li.cil.oc.api.INetworkNode

/**
 * Mixin for network nodes.
 *
 * This provides more Scala-like member names and adds some default
 * implementations. It also provides event callbacks for network joins and
 * leaves, as well as address changes.
 */
trait NetworkNode extends INetworkNode {
  var network: Option[INetwork] = None

  private var address_ = 0

  def address = address_

  def address_=(value: Int) = if (value != address_) {
    address_ = value
    onAddressChange()
  }

  def receive(message: INetworkMessage) {}

  def onConnect() {}

  def onDisconnect() {}

  def onAddressChange() {}

  // ----------------------------------------------------------------------- //

  def getAddress = address

  def setAddress(value: Int) = address = value

  def getNetwork = network.orNull

  def setNetwork(n: INetwork) = {
    if (network.isDefined) {
      network = None
      onDisconnect()
    }
    network = Option(n)
    if (network.isDefined) {
      onConnect()
    }
  }
}