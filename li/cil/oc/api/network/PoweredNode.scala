package li.cil.oc.api.network

import li.cil.oc.common.tileentity.PowerDistributor
import scala.collection.mutable


trait PoweredNode extends Node {
  var powerDistributors = mutable.Set[PowerDistributor]()


  override def receive(message: Message): Option[Array[Any]] = {
    message.name match {
      case "network.connect" => {
        message.source match {
          case distributor: PowerDistributor => {
            println("connect")
            if (powerDistributors.contains(istributor)) {
              powerDistributors += distributor
              distributor.connectNode(this, _demand, _priority)
            }
          }
          case _ =>
        }
      }
      case "network.disconnect" => {
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

  private var _priority = 0

  def priority = _priority


  def main: PowerDistributor = {
    powerDistributors.filter(p => p.isActive).foreach(f => return f)


  }
}

