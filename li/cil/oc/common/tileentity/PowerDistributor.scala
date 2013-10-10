package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{PoweredNode, Message, Visibility}
import scala.collection.mutable.ArrayBuffer


class PowerDistributor extends Rotatable with PoweredNode {

  var isActive = true
  var energyStorageList = ArrayBuffer[EnergyStorage]()
  var energyDemand = 0
  demand = 1

  override def name = "powerdistributor"

  override def visibility = Visibility.Network


  override def receive(message: Message): Option[Array[Any]] = {

    message.name match {
      case "network.connect" => {
        message.source match {
          case distributor: PowerDistributor =>
            //if other powerDistributor connected and is active set inactive
            if (message.source != this && distributor.isActive) {
              isActive = false

              println("demand now (disabled) " + 0)
            }
          case _ =>
        }
      }
      case "power.find" => {
        message.source match {
          case distributor: PowerDistributor =>
            //received request from other distributor that is newly connected... set it to inactive

            if (isActive && message.source != this) {
              distributor.isActive = false
            }
          case _ =>
        }
      }
      case "network.disconnect" => {
        message.source match {
          case distributor: PowerDistributor =>
            println("distri disc recieved")
            if (distributor.isActive && distributor != this) {
              isActive = true
              network.foreach(_.sendToVisible(this, "power.find"))

              println("demand now (new main) " + energyDemand)
            }
          case _ =>
        }

      }
      case _ => // Ignore.
    }
    super.receive(message)

  }

  def connectNode(node: PoweredNode, amount: Int, priority: Int) {
    if (energyStorageList.filter(x => x.node == node).isEmpty) {
      energyStorageList += new EnergyStorage(node, amount, priority)
      energyDemand += amount
    }

    if (isActive)
      println("demand now (connect)" + energyDemand)
  }

  /**
   * Updates the demand of the node to the given value
   * @param node
   * @param demand
   */
  def updateDemand(node: PoweredNode, demand: Int) {
    energyStorageList.filter(n => n.node == node).foreach(n => {
      energyDemand -= n.amount
      energyDemand += demand
      n.amount = demand
    })
    if (isActive)
      println("demand now (update)" + energyDemand)
  }

  def disconnectNode(node: PoweredNode) {
    energyStorageList.clone().foreach(e => {
      if (e == null || node == null) {
        println("something null")

      }
      else if (e.node == node) {
        energyStorageList -= e
        energyDemand -= e.amount
      }

    })
    if (isActive)
      println("demand now (disc) " + energyDemand)
  }

  override protected def onConnect() {
    //check if other distributors already are in the network
    network.foreach(_.sendToVisible(this, "power.find"))
    super.onConnect()
  }

  override protected def onDisconnect() {
    println("disc distri other " + arrayBuffer.length)
    super.onDisconnect()
    energyStorageList.clone().foreach(e => {

      e.node.removeBuffer(this)
      if (energyStorageList.contains(e)) {

        energyStorageList -= e
        energyDemand -= e.amount
      }

    })
    if (isActive)
      println("demand now (close) " + energyDemand)
  }

  override def updateEntity() {
    super.updateEntity()
    if (isActive) {

    }
    //TODO remove energy
  }

  class EnergyStorage(var node: PoweredNode, var amount: Int, var priority: Int) {

  }

}
