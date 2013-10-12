package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{PoweredNode, Message, Visibility}
import scala.collection.mutable
import li.cil.oc.common.tileentity.Rotatable


class PowerDistributor extends Rotatable with PoweredNode {

  var isActive = true
  var energyStorageList = mutable.Set[EnergyStorage]()
  var energyDemand = 0
  var storedEnergy = 0
  var MAXENERGY = 2000

  override val name = "powerdistributor"

  override val visibility = Visibility.Network


  override def receive(message: Message): Option[Array[Any]] = {
    if (message.source != this)
    {message.name match {
      case "system.connect" => {
        message.source match {
          case distributor: PowerDistributor =>
            //if other powerDistributor connected and is active set inactive
            if (distributor.isActive) {
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

            if (isActive ) {
              return Result
            }
          case _ =>
        }
      }
      case "system.disconnect" => {
        message.source match {
          case distributor: PowerDistributor =>
            println("distri disc recieved")
            if (distributor.isActive ) {
              isActive = true
              network.foreach(_.sendToVisible(this, "power.find"))

              println("demand now (new main) " + energyDemand)
            }
          case _ =>
        }

      }
      case _ => // Ignore.
    }
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



  override def updateEntity() {
    super.updateEntity()
    if (isActive) {

    }
    //TODO remove energy
  }
  def getDemand = {
    MAXENERGY-storedEnergy max 0
  }
  def addEnergy(amount:Int){
    storedEnergy+=amount
  }
  class EnergyStorage(var node: PoweredNode, var amount: Int, var priority: Int) {

  }

}
