package li.cil.oc.api.network

import li.cil.oc.common.tileentity.PowerDistributor
import scala.collection.mutable
import net.minecraft.nbt.NBTTagCompound


trait Receiver extends Node {
  var powerDistributors = mutable.Set[PowerDistributor]()

  private var _demand = 2.0

  def demand = _demand

  def demand_=(value: Double) = {

    powerDistributors.foreach(e => e.updateDemand(this, value))
    _demand = value
  }

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



  def main: PowerDistributor = {
    powerDistributors.find(p => p.isActive) match {
      case Some(p: PowerDistributor) => p
      case _ => null
    }


  }
  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    buffer = nbt.getDouble("buffer")
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)
    nbt.setDouble("buffer",buffer)
  }

  private var buffer = 0.0
  private val maxBuffer = 100.0
  private var hasEnergy = false
  private var isConnected = false

  override def update() {
    super.update()
    //if has enough energy to operate
    if (isConnected) {
      //increase buffer
      if (maxBuffer > buffer + 1)
        buffer += 1
      //notify if energy wasn't available before
      if (!hasEnergy) {
        hasEnergy = true
        onPowerAvailable()
      }
    }
    else {
      //continue running until we are out of energy
      if (buffer - demand < 0) {
        if (hasEnergy) {
          hasEnergy = false
          onPowerLoss()
        }
      } else {
        buffer -= demand
      }
    }
  }

  /**
   * called from the producer when he has enough energy to operate
   */
  final def connect() {
    isConnected = true
  }

  /**
   * Called from the producer when there is not enough energy to operate
   */
  final def unConnect() {
    isConnected = false
  }

  /**
   * Called when the receiver has enough power to operate.
   */
  def onPowerAvailable() {
    println("received energy")
  }

  /**
   * Called when the receiver has no power to operate. This can happen at a later time
   * then unConnect was called, because of the internal capacity
   */
  def onPowerLoss() {
    println("no more energy")
  }
}

