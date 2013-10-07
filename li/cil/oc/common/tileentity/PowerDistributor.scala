package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{PoweredNode, Message, Visibility, Node}
import scala.collection.mutable.ArrayBuffer


class PowerDistributor extends Rotatable with PoweredNode {


  var arrayBuffer = ArrayBuffer[EnergyStorage]()
  var energyDemand = 0
  demand = 1

  override def name = "powerdistributor"

  override def visibility = Visibility.Network


  override def receive(message: Message): Option[Array[Any]] = {

    message.name match {
      case "network.disconnect" => {
        if (message.source == main) {
          main = this
          network.foreach(_.sendToAddress(this, address.get, "power.request", 1, -1))
          network.foreach(_.sendToVisible(this, "power.connect"))
        }
        disconnectNode(message.source)

      }
      case _ => // Ignore.
    }
    val ret = super.receive(message)
    message.name match {
      case "network.connect" => {
        if (main == this) {
          network.foreach(_.sendToAddress(this, message.source.address.get, "power.connect"))
        }
      }
      case "power.find" => {
        if (main == this) {
          network.foreach(_.sendToAddress(this, message.source.address.get, "power.connect"))
          message.cancel()
        }
      }
      case "power.request" => {
        if (main == this) {

          message.data match {
            case Array(value: Int, priority: Int) => {
              if (arrayBuffer.filter(_.node == message.source).isEmpty) {
                arrayBuffer += new EnergyStorage(message.source, value, priority)

                energyDemand += value
                println("demand now " + energyDemand)
              }
            }
            case _ => println("unknown format")
          }
        }
      }
      case "power.disconnect" => {
        println("received disc asd")
        disconnectNode(message.source)

      }
      case _ => // Ignore.
    }
    ret
  }
  def disconnectNode(node:Node){
    arrayBuffer.clone().foreach(e => {
      if (e == null || node == null) {
        println("something null")

      }
      else if (e.node == node) {
        arrayBuffer -= e
        energyDemand -= e.amount
      }

    })
    println("demand now after " + energyDemand)
  }
  override protected def onConnect() {
    network.foreach(_.sendToVisible(this, "power.find"))
    if (main == null) {
      main = this
      network.foreach(_.sendToAddress(this, address.get, "power.request", demand, 0))
    }
    super.onConnect()
  }

  class EnergyStorage(var node: Node, var amount: Int, var priority: Int) {

  }

}
