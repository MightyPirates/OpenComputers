package li.cil.oc.api.power

import li.cil.oc.api.network.{Message, Node}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable

trait Provider {
//
//  var isActive = false
//  var updateNodes = false
//  var energyDemand = 0.0
//  var storedEnergy = 0.0
//  var MAXENERGY: Double = 2000.0
//
//  var energyStorageList = mutable.Set[EnergyStorage]()
//
//  override def receive(message: Message): Array[AnyRef] = Option(super.receive(message)).orElse {
//    if (message.source != this) {
//      message.name match {
//        case "system.connect" => {
//          message.source match {
//            case distributor: Provider =>
//              //if other powerDistributor connected and is active set inactive
//              if (distributor.isActive) {
//                isActive = false
//                println("demand now (disabled) " + 0)
//              }
//            case _ =>
//          }
//        }
//        case "power.find" => {
//          message.source match {
//            case distributor: Provider =>
//              if (isActive) {
//                message.cancel()
//                return Array(this)
//              }
//            case _ =>
//          }
//        }
//        case "system.disconnect" => {
//          message.source match {
//            case distributor: Provider =>
//              println("distri disc recieved")
//              if (distributor.isActive) {
//                searchMain()
//              }
//            case _ =>
//          }
//        }
//        case _ => // Ignore.
//      }
//    }
//    None
//  }.orNull
//
//  override protected def onConnect() {
//    //check if other distributors already are in the network
//    searchMain()
//    super.onConnect()
//  }
//
//  override abstract def readFromNBT(nbt: NBTTagCompound) = {
//    super.load(nbt)
//    storedEnergy = nbt.getDouble("storedEnergy")
//  }
//
//  override abstract def writeToNBT(nbt: NBTTagCompound) = {
//    super.save(nbt)
//    nbt.setDouble("storedEnergy", storedEnergy)
//  }
//
//  /**
//   * Connect a reciever to the provider
//   * @param receiver
//   * @param amount
//   */
//  def connectNode(receiver: Receiver, amount: Double) {
//    if (!energyStorageList.exists(_.node == receiver)) {
//      energyStorageList += new EnergyStorage(receiver, amount)
//      energyDemand += amount
//      updateNodes = true
//    }
//
//    if (isActive)
//      println("demand now (connect)" + energyDemand)
//  }
//
//  /**
//   * Updates the demand of the node to the given value
//   * @param receiver
//   * @param demand
//   */
//  def updateDemand(receiver: Receiver, demand: Double) {
//    energyStorageList.filter(n => n.node == receiver).foreach(n => {
//      energyDemand -= n.amount
//      energyDemand += demand
//      n.amount = demand
//    })
//    if (isActive)
//      println("demand now (update)" + energyDemand)
//  }
//
//  def disconnectNode(receiver: Receiver) {
//    energyStorageList.clone().foreach(e => {
//      if (e == null || receiver == null) {
//        println("something null")
//
//      }
//      else if (e.node == receiver) {
//        energyStorageList -= e
//        energyDemand -= e.amount
//        if (isActive) {
//          receiver.isReceivingPower = false
//          updateNodes = true
//        }
//      }
//
//    })
//    if (isActive)
//      println("demand now (disc) " + energyDemand)
//  }
//
//  private var hasEnergy = false

//  override def update() {
//    super.update()
//    //check if is main
//    if (isActive) {
//      //if enough energy is available to supply all receivers
//      if (storedEnergy > energyDemand) {
//        storedEnergy -= energyDemand
//        if (!hasEnergy)
//          updateNodes = true
//        hasEnergy = true
//        println("energy level now " + storedEnergy)
//      }
//      else {
//        if (hasEnergy)
//          updateNodes = true
//        hasEnergy = false
//      }
//      //if nodes must be updated send message to them
//      if (updateNodes) {
//        if (hasEnergy)
//          energyStorageList.foreach(storage => storage.node.isReceivingPower = true)
//        else
//          energyStorageList.foreach(storage => storage.node.isReceivingPower = false)
//        updateNodes = false
//      }
//    }
//  }
//
//  def getDemand = {
//    MAXENERGY - storedEnergy max 0.0
//  }
//
//  def addEnergy(amount: Double) {
//    storedEnergy += amount
//  }
//
//  def searchMain() {
//    network.foreach(_.sendToReachable(this, "power.find") match {
//      case Array(powerDistributor: Provider) => {
//        println("found other distri")
//        isActive = false
//      }
//      case _ => {
//        println("no other")
//        isActive = true
//        updateNodes = true
//        println("demand now (new main) " + energyDemand)
//      }
//    })
//  }
//
//  class EnergyStorage(var node: Receiver, var amount: Double)
//
}
