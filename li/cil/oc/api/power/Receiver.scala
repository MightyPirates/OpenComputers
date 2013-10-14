package li.cil.oc.api.power

import li.cil.oc.api.network.{Message, Node}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable

trait Receiver extends Node {
  def demand = _demand

  def demand_=(value: Double) = if (value != _demand) {
    providers.foreach(_.updateDemand(this, value))
    _demand = value
  }

  def provider: Option[Provider] = providers.find(_.isActive)

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
  def onPowerUnavailable() {
    println("no more energy")
  }

  // ----------------------------------------------------------------------- //

  override def receive(message: Message) = super.receive(message) orElse {
    message.name match {
      case "system.connect" => {
        message.source match {
          case p: Provider => {
            if (providers.add(p)) {
              p.connectNode(this, _demand)
            }
          }
          case _ =>
        }
      }
      case "system.disconnect" => {
        message.source match {
          case p: Provider => {
            if (providers.remove(p)) {
              p.disconnectNode(this)
            }
          }
          case _ =>
        }
      }
      case _ =>
    }
    None
  }

  override protected def onDisconnect() {
    super.onDisconnect()
    providers.foreach(_.disconnectNode(this))
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    buffer = nbt.getDouble("buffer")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("buffer", buffer)
  }

  // ----------------------------------------------------------------------- //

  private var _demand = 2.0
  private val providers = mutable.Set.empty[Provider]
  private var buffer = 0.0
  private val maxBuffer = 100.0
  private var isPowerAvailable = false

  /** Set from the provider whenever its power state changes. */
  private[power] var isReceivingPower = false

  override def update() {
    super.update()
    //if has enough energy to operate
    if (isReceivingPower) {
      //increase buffer
      // TODO maybe make the speed of the "cooldown" dependent on the demand?
      // TODO another possibility: increase the demand dynamically while charging?
      if (maxBuffer > buffer + 1)
        buffer += 1
      //notify if energy wasn't available before
      if (!isPowerAvailable) {
        isPowerAvailable = true
        onPowerAvailable()
      }
    }
    //continue running until we are out of energy
    else if (buffer >= demand) {
      buffer -= demand
    }
    else if (isPowerAvailable) {
      isPowerAvailable = false
      onPowerUnavailable()
    }
  }
}

