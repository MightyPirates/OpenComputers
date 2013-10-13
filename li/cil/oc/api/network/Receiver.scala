package li.cil.oc.api.network

import li.cil.oc.common.tileentity.PowerDistributor
import li.cil.oc.api.network.{Visibility, Node, Message}
import scala.collection.mutable


trait Receiver extends Node {
  var powerDistributors = mutable.Set[PowerDistributor]()


  override def receive(message: Message): Option[Array[Any]] = {
    message.name match {
      case "system.connect" => {
        message.source match {
          case distributor: PowerDistributor => {
            println("connect")
            if (!powerDistributors.contains(distributor)) {
              powerDistributors += distributor
              distributor.connectNode(this, _demand)
            }
          }
          case _ =>
        }
      }
      case "system.disconnect" => {
        message.source match {
          case distributor: PowerDistributor => {
            println("connect")
            if (powerDistributors.contains(distributor)) {
              powerDistributors -= distributor
              distributor.disconnectNode(this)
            }
          }
          case _ =>
        }
      }
      case _ =>
    }
    super.receive(message)
  }


  override protected def onDisconnect() {
    super.onDisconnect()
    powerDistributors.foreach(e => {
      e.disconnectNode(this)
    })

  }

  private var _demand = 0

  def demand = _demand

  def demand_=(value: Int) = {

    powerDistributors.foreach(e => e.updateDemand(this, value))
    _demand = value
  }


  def main: PowerDistributor = {
    powerDistributors.find(p => p.isActive) match {
      case Some(p:PowerDistributor) => p
      case _=> null
    }


  }

  def onPowerAvailable()
  def onPowerLoss()
}

