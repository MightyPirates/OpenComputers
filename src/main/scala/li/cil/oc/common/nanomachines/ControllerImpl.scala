package li.cil.oc.common.nanomachines

import java.lang
import java.util.UUID

import com.google.common.base.Charsets
import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.nanomachines.Controller
import li.cil.oc.api.network.Packet
import li.cil.oc.api.network.WirelessEndpoint
import li.cil.oc.server.PacketSender
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.PlayerUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.EnumParticleTypes
import net.minecraft.world.World

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class ControllerImpl(val player: EntityPlayer) extends Controller with WirelessEndpoint {
  if (isServer) api.Network.joinWirelessNetwork(this)

  final val MaxSenderDistance = 2f
  final val FullSyncInterval = 20 * 60

  var uuid = UUID.randomUUID.toString
  var responsePort = 0
  var storedEnergy = Settings.get.bufferNanomachines * 0.25
  var hadPower = true
  val configuration = new NeuralNetwork(this)
  val activeBehaviors = mutable.Set.empty[Behavior]
  var activeBehaviorsDirty = true
  var configCooldown = 0
  var hasSentConfiguration = false

  override def world: World = player.getEntityWorld

  override def x: Int = BlockPosition(player).x

  override def y: Int = BlockPosition(player).y

  override def z: Int = BlockPosition(player).z

  override def receivePacket(packet: Packet, sender: WirelessEndpoint): Unit = {
    if (getLocalBuffer > 0) {
      val (dx, dy, dz) = ((sender.x + 0.5) - player.posX, (sender.y + 0.5) - player.posY, (sender.z + 0.5) - player.posZ)
      val dSquared = dx * dx + dy * dy + dz * dz
      if (dSquared < MaxSenderDistance * MaxSenderDistance) packet.data.headOption match {
        case Some(header: Array[Byte]) if new String(header, Charsets.UTF_8) == "nanomachines" =>
          val command = packet.data.drop(1).map {
            case value: Array[Byte] => new String(value, Charsets.UTF_8)
            case value => value
          }
          command match {
            case Array("setResponsePort", port: java.lang.Number) =>
              responsePort = port.intValue max 0 min 0xFFFF
              respond(sender, "responsePort", responsePort)
            case Array("dispose") =>
              api.Nanomachines.uninstallController(player)
              respond(sender, "disposed")
            case Array("reconfigure") =>
              reconfigure()
              respond(sender, "reconfigured")
            case Array("getTotalInputCount") =>
              respond(sender, "totalInputCount", getTotalInputCount)
            case Array("getSafeInputCount") =>
              respond(sender, "safeInputCount", getSafeInputCount)
            case Array("getInput", index: java.lang.Number) =>
              try {
                val trigger = getInput(index.intValue - 1)
                respond(sender, "input", index.intValue, trigger)
              }
              catch {
                case _: Throwable =>
                  respond(sender, "input", "error")
              }
            case Array("setInput", index: java.lang.Number, value: java.lang.Boolean) =>
              try {
                setInput(index.intValue - 1, value.booleanValue)
                respond(sender, "input", index.intValue, getInput(index.intValue - 1))
              }
              catch {
                case _: Throwable =>
                  respond(sender, "input", "error")
              }
            case Array("getActiveEffects") =>
              configuration.synchronized {
                val names = getActiveBehaviors.map(_.getNameHint).filterNot(Strings.isNullOrEmpty)
                val joined = "{" + names.map(_.replace(',', '_').replace('"', '_')).mkString(",") + "}"
                respond(sender, "active", joined)
              }
            case Array("getPowerState") =>
              respond(sender, "power", getLocalBuffer, getLocalBufferSize)
            case _ => // Ignore.
          }
        case _ => // Not for us.
      }
    }
  }

  def respond(endpoint: WirelessEndpoint, data: Any*): Unit = {
    if (responsePort > 0) {
      val cost = Settings.get.wirelessCostPerRange * 10
      val epsilon = 0.1
      if (changeBuffer(-cost) > -epsilon) {
        val packet = api.Network.newPacket(uuid, null, responsePort, (Iterable("nanomachines") ++ data.map(_.asInstanceOf[AnyRef])).toArray)
        api.Network.sendWirelessPacket(this, 10, packet)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def reconfigure() = {
    if (isServer && configCooldown < 1) configuration.synchronized {
      configuration.reconfigure()
      activeBehaviorsDirty = true
      configCooldown = (Settings.get.nanomachineReconfigureTimeout * 20).toInt

      player match {
        case playerMP: EntityPlayerMP if playerMP.playerNetServerHandler != null =>
          player.addPotionEffect(new PotionEffect(Potion.blindness.id, 100))
          player.addPotionEffect(new PotionEffect(Potion.poison.id, 150))
          player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 200))
          changeBuffer(-Settings.get.nanomachineReconfigureCost)

          hasSentConfiguration = false
        case _ => // We're still setting up / loading.
      }
    }
    this
  }

  override def getTotalInputCount: Int = configuration.synchronized(configuration.triggers.length)

  override def getSafeInputCount: Int = Settings.get.nanomachinesSafeInputCount

  override def getInput(index: Int): Boolean = configuration.synchronized(configuration.triggers(index).isActive)

  override def setInput(index: Int, value: Boolean): Unit = {
    if (isServer && configCooldown < 1) configuration.synchronized {
      configuration.triggers(index).isActive = value
      activeBehaviorsDirty = true
    }
  }

  override def getActiveBehaviors: lang.Iterable[Behavior] = configuration.synchronized {
    cleanActiveBehaviors()
    activeBehaviors
  }

  override def getInputCount(behavior: Behavior): Int = configuration.synchronized(configuration.inputs(behavior))

  // ----------------------------------------------------------------------- //

  override def getLocalBuffer: Double = storedEnergy

  override def getLocalBufferSize: Double = Settings.get.bufferNanomachines

  override def changeBuffer(delta: Double): Double = {
    if (isClient) delta
    else if (delta < 0 && (Settings.get.ignorePower || player.capabilities.isCreativeMode)) 0.0
    else {
      val newValue = storedEnergy + delta
      storedEnergy = math.min(math.max(newValue, 0), getLocalBufferSize)
      newValue - storedEnergy
    }
  }

  // ----------------------------------------------------------------------- //

  def update(): Unit = {
    if (isServer) {
      api.Network.updateWirelessNetwork(this)
    }

    if (configCooldown > 0) {
      configCooldown -= 1
      return
    }

    val hasPower = getLocalBuffer > 0 || Settings.get.ignorePower
    lazy val active = getActiveBehaviors.toIterable // Wrap once.
    lazy val activeInputs = configuration.triggers.count(_.isActive)

    if (hasPower != hadPower) {
      if (!hasPower) active.foreach(_.onDisable())
      else active.foreach(_.onEnable())
    }

    if (hasPower) {
      active.foreach(_.update())

      if (isServer) {
        if (player.getEntityWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
          changeBuffer(-Settings.get.nanomachineCost * Settings.get.tickFrequency * (activeInputs + 0.5))
          PacketSender.sendNanomachinePower(player)
        }

        val overload = activeInputs - getSafeInputCount
        if (!player.capabilities.isCreativeMode && overload > 0 && player.getEntityWorld.getTotalWorldTime % 20 == 0) {
          player.setHealth(player.getHealth - overload)
          player.performHurtAnimation()
        }
      }

      if (isClient) {
        val energyRatio = getLocalBuffer / (getLocalBufferSize + 1)
        val triggerRatio = activeInputs / (configuration.triggers.length + 1)
        val intensity = (energyRatio + triggerRatio) * 0.25
        PlayerUtils.spawnParticleAround(player, EnumParticleTypes.PORTAL, intensity)
      }
    }

    if (isServer) {
      // Send new power state, if it changed.
      if (hadPower != hasPower) {
        PacketSender.sendNanomachinePower(player)
      }

      // Send a full sync every now and then, e.g. for other players coming
      // closer that weren't there to get the initial info for an enabled
      // input.
      if (!hasSentConfiguration || player.getEntityWorld.getTotalWorldTime % FullSyncInterval == 0) {
        hasSentConfiguration = true
        PacketSender.sendNanomachineConfiguration(player)
      }
    }

    hadPower = hasPower
  }

  def reset(): Unit = {
    configuration.synchronized {
      for (index <- 0 until getTotalInputCount) {
        configuration.triggers(index).isActive = false
        activeBehaviorsDirty = true
      }
    }
  }

  def dispose(): Unit = {
    reset()
    if (isServer) {
      api.Network.leaveWirelessNetwork(this)
      PacketSender.sendNanomachineConfiguration(player)
    }
  }

  // ----------------------------------------------------------------------- //

  def save(nbt: NBTTagCompound): Unit = configuration.synchronized {
    nbt.setString("uuid", uuid)
    nbt.setInteger("port", responsePort)
    nbt.setDouble("energy", storedEnergy)
    nbt.setNewCompoundTag("configuration", configuration.save)
  }

  def load(nbt: NBTTagCompound): Unit = configuration.synchronized {
    uuid = nbt.getString("uuid")
    responsePort = nbt.getInteger("port")
    storedEnergy = nbt.getDouble("energy")
    configuration.load(nbt.getCompoundTag("configuration"))
    activeBehaviorsDirty = true
  }

  // ----------------------------------------------------------------------- //

  private def isClient = world.isRemote

  private def isServer = !isClient

  private def cleanActiveBehaviors(): Unit = {
    if (activeBehaviorsDirty) {
      configuration.synchronized(if (activeBehaviorsDirty) {
        val newBehaviors = configuration.behaviors.filter(_.isActive).map(_.behavior)
        val addedBehaviors = newBehaviors -- activeBehaviors
        val removedBehaviors = activeBehaviors -- newBehaviors
        activeBehaviors.clear()
        activeBehaviors ++= newBehaviors
        activeBehaviorsDirty = false
        addedBehaviors.foreach(_.onEnable())
        removedBehaviors.foreach(_.onDisable())

        if (isServer) {
          PacketSender.sendNanomachineInputs(player)
        }
      })
    }
  }
}
