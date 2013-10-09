package li.cil.oc.api.network

import li.cil.oc.common.tileentity.PowerDistributor
import scala.collection.mutable.ArrayBuffer


trait PoweredNode extends Node {
  var arrayBuffer = ArrayBuffer[PowerDistributor]()
  var demand = 2

  override def receive(message: Message): Option[Array[Any]] = {
    message.name match {
      case "network.connect" => {
        message.source match {
          case distributor: PowerDistributor => {
            println("connect")
            if (arrayBuffer.filter(p => p == distributor).isEmpty) {
              arrayBuffer += distributor
              distributor.connectNode(this, getDemand, getPriority)
            }
          }
          case _ =>
        }
      }
      case "network.disconnect" => {
        message.source match {
          case distributor: PowerDistributor => {
            println("connect")
            if (arrayBuffer.contains(distributor)) {
              arrayBuffer -= distributor
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

  def removeBuffer(distributor: PowerDistributor) = {
    distributor.disconnectNode(this)
    arrayBuffer -= distributor
  }

  override protected def onDisconnect() {

    arrayBuffer.foreach(e => {
      e.disconnectNode(this)
    })
    super.onDisconnect()
  }

  def getDemand: Int = 2

  def getPriority: Int = 1

  def updateDemand(demand: Int) {
    arrayBuffer.foreach(e => e.updateDemand(this, getDemand))
  }
}
