package li.cil.oc.server.computer

import li.cil.oc.api.INetwork
import li.cil.oc.api.INetworkMessage
import li.cil.oc.api.INetworkNode

trait NetworkNode extends INetworkNode {
  var network: Option[INetwork] = None

  private var address_ = 0

  def address = address_

  def address_=(value: Int) = if (value != address_) {
    address_ = value
    onAddressChange()
  }

  def onConnect() {}

  def onDisconnect() {}

  def onAddressChange() {}

  override def receive(message: INetworkMessage) {}

  // ----------------------------------------------------------------------- //

  override def getAddress = address

  override def setAddress(value: Int) = address = value

  override def getNetwork = network.orNull

  override def setNetwork(n: INetwork) = {
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