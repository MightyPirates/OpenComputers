package li.cil.oc.api.network

import scala.collection.mutable


trait Provider extends  Node{

  var isActive = false
  var energyDemand =0.0
  var storedEnergy =0.0
  var MAXENERGY :Double
  var energyStorageList = mutable.Set[EnergyStorage]()
  override def receive(message: Message): Option[Array[Any]] = {
    if (message.source != this) {
      message.name match {
        case "system.connect" => {
          message.source match {
            case distributor: Provider =>
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
            case distributor: Provider =>
              if (isActive) {
                message.cancel()
                return result(this)
              }
            case _ =>
          }
        }
        case "system.disconnect" => {
          message.source match {
            case distributor: Provider =>
              println("distri disc recieved")
              if (distributor.isActive) {

                searchMain()

              }
            case _ =>
          }

        }
        case _ => // Ignore.
      }
    }
    super.receive(message)

  }

  def connectNode(node: Receiver, amount: Int, priority: Int) {
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
  def updateDemand(node: Receiver, demand: Int) {
    energyStorageList.filter(n => n.node == node).foreach(n => {
      energyDemand -= n.amount
      energyDemand += demand
      n.amount = demand
    })
    if (isActive)
      println("demand now (update)" + energyDemand)
  }

  def disconnectNode(node: Receiver) {
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
    searchMain()
    super.onConnect()
  }


   def update() {
    //super.updateEntity()
    if (isActive) {
      if(storedEnergy>energyDemand){
        storedEnergy-=energyDemand
        println("energy level now "+storedEnergy)
      }
    }
    //TODO remove energy
  }

  def getDemand = {
    MAXENERGY - storedEnergy max 0.0
  }

  def addEnergy(amount: Double) {
    storedEnergy += amount


  }

  def searchMain() {
    network.foreach(_.sendToVisible(this, "power.find") match {
      case Some(Array(powerDistributor: Provider)) => {
        println("found other distri")
        isActive = false
      }
      case _ => {
        println("no other")
        isActive = true

        println("demand now (new main) " + energyDemand)
      }
    })
  }

  class EnergyStorage(var node: Receiver, var amount: Int) {

  }
}
