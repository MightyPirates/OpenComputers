package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.nanomachines.Controller
import li.cil.oc.api.network._
import li.cil.oc.common.Slot
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.tileentity.capabilities.{RedstoneAwareImpl, RotatableImpl}
import li.cil.oc.common.tileentity.traits.RedstoneAwareImpl
import li.cil.oc.integration.util.ItemCharge
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Charger extends traits.Environment with traits.PowerAcceptor with RedstoneAwareImpl with RotatableImpl with traits.ComponentInventory with traits.Tickable with Analyzable with traits.StateAware with DeviceInfo {
  val getNode = api.Network.newNode(this, Visibility.NONE).
    withConnector(Settings.get.bufferConverter).
    create()

  val connectors = mutable.Set.empty[Chargeable]

  var chargeSpeed = 0.0

  var hasPower = false

  var invertSignal = false

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Charger",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "PowerUpper"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = side != facing

  override protected def connector(side: EnumFacing) = Option(if (side != facing) getNode else null)

  override def energyThroughput = Settings.get.chargerRate

  override def getCurrentState = {
    // TODO Refine to only report working if present robots/drones actually *need* power.
    if (connectors.nonEmpty) {
      if (hasPower) util.EnumSet.of(api.util.StateAware.State.IsWorking)
      else util.EnumSet.of(api.util.StateAware.State.CanWork)
    }
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    player.sendMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed))
    null
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()

    // Offset by hashcode to avoid all chargers ticking at the same time.
    if ((getWorld.getWorldInfo.getWorldTotalTime + math.abs(hashCode())) % 20 == 0) {
      updateConnectors()
    }

    if (isServer && getWorld.getWorldInfo.getWorldTotalTime % Settings.get.tickFrequency == 0) {
      var canCharge = Settings.get.ignorePower

      // Charging of external devices.
      {
        val charge = Settings.get.chargeRateExternal * chargeSpeed * Settings.get.tickFrequency
        canCharge ||= charge > 0 && getNode.getGlobalBuffer >= charge * 0.5
        if (canCharge) {
          connectors.foreach(connector => getNode.changeBuffer(connector.changeBuffer(charge + getNode.changeBuffer(-charge))))
        }
      }

      // Charging of internal devices.
      {
        val charge = Settings.get.chargeRateTablet * chargeSpeed * Settings.get.tickFrequency
        canCharge ||= charge > 0 && getNode.getGlobalBuffer >= charge * 0.5
        if (canCharge) {
          (0 until getSizeInventory).map(getStackInSlot).foreach(stack => if (stack != null) {
            val offered = charge + getNode.changeBuffer(-charge)
            val surplus = ItemCharge.charge(stack, offered)
            getNode.changeBuffer(surplus)
          })
        }
      }

      if (hasPower && !canCharge) {
        hasPower = false
        ServerPacketSender.sendChargerState(this)
      }
      if (!hasPower && canCharge) {
        hasPower = true
        ServerPacketSender.sendChargerState(this)
      }
    }

    if (isClient && chargeSpeed > 0 && hasPower && getWorld.getWorldInfo.getWorldTotalTime % 10 == 0) {
      connectors.foreach(connector => {
        val position = connector.pos
        val theta = getWorld.rand.nextDouble * Math.PI
        val phi = getWorld.rand.nextDouble * Math.PI * 2
        val dx = 0.45 * Math.sin(theta) * Math.cos(phi)
        val dy = 0.45 * Math.sin(theta) * Math.sin(phi)
        val dz = 0.45 * Math.cos(theta)
        getWorld.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, position.xCoord + dx, position.yCoord + dz, position.zCoord + dy, 0, 0, 0)
      })
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.getNode) {
      onNeighborChanged()
    }
  }

  // ----------------------------------------------------------------------- //

  private final val ChargeSpeedTag = Settings.namespace + "chargeSpeed"
  private final val ChargeSpeedTagCompat = "chargeSpeed"
  private final val HasPowerTag = Settings.namespace + "hasPower"
  private final val HasPowerTagCompat = "hasPower"
  private final val InvertSignalTag = Settings.namespace + "invertSignal"
  private final val InvertSignalTagCompat = "invertSignal"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(ChargeSpeedTagCompat))
      chargeSpeed = nbt.getDouble(ChargeSpeedTagCompat) max 0 min 1
    else
      chargeSpeed = nbt.getDouble(ChargeSpeedTag) max 0 min 1
    if (nbt.hasKey(HasPowerTagCompat))
      hasPower = nbt.getBoolean(HasPowerTagCompat)
    else
      hasPower = nbt.getBoolean(HasPowerTag)
    if (nbt.hasKey(InvertSignalTagCompat))
      invertSignal = nbt.getBoolean(InvertSignalTagCompat)
    else
      invertSignal = nbt.getBoolean(InvertSignalTag)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setDouble(ChargeSpeedTag, chargeSpeed)
    nbt.setBoolean(HasPowerTag, hasPower)
    nbt.setBoolean(InvertSignalTag, invertSignal)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    chargeSpeed = nbt.getDouble(ChargeSpeedTag)
    hasPower = nbt.getBoolean(HasPowerTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble(ChargeSpeedTag, chargeSpeed)
    nbt.setBoolean(HasPowerTag, hasPower)
  }

  // ----------------------------------------------------------------------- //

  override def isComponentSlot(slot: Int, stack: ItemStack): Boolean =
    super.isComponentSlot(slot, stack) && (Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver) => driver.slot(stack) == Slot.Tablet
      case _ => false
    })

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, Some(driver)) if driver.slot(stack) == Slot.Tablet => true
    case _ => ItemCharge.canCharge(stack)
  }

  // ----------------------------------------------------------------------- //

  override def updateRedstoneInput(side: EnumFacing) {
    super.updateRedstoneInput(side)
    val signal = math.max(0, math.min(15, EnumFacing.values.map(input).max))

    if (invertSignal) chargeSpeed = (15 - signal) / 15.0
    else chargeSpeed = signal / 15.0
    if (isServer) {
      ServerPacketSender.sendChargerState(this)
    }
  }

  def onNeighborChanged() {
    checkRedstoneInputChanged()
    updateConnectors()
  }

  def updateConnectors() {
    val robots = EnumFacing.values.map(side => {
      val blockPos = BlockPosition(this).offset(side)
      if (getWorld.blockExists(blockPos)) Option(getWorld.getTileEntity(blockPos))
      else None
    }).collect {
      case Some(t: RobotProxy) => new RobotChargeable(t.robot)
    }
    val bounds = BlockPosition(this).bounds.expand(1, 1, 1)
    val drones = getWorld.getEntitiesWithinAABB(classOf[Drone], bounds).collect {
      case drone: Drone => new DroneChargeable(drone)
    }

    val players = getWorld.getEntitiesWithinAABB(classOf[EntityPlayer], bounds).collect {
      case player: EntityPlayer if api.Nanomachines.hasController(player) => new PlayerChargeable(player)
    }

    // Only update list when we have to, keeps pointless block updates to a minimum.

    val newConnectors = robots ++ drones ++ players
    if (connectors.size != newConnectors.length || (connectors.nonEmpty && (connectors -- newConnectors).nonEmpty)) {
      connectors.clear()
      connectors ++= newConnectors
      getWorld.notifyNeighborsOfStateChange(getPos, getBlockType, false)
    }
  }

  trait Chargeable {
    def pos: Vec3d

    def changeBuffer(delta: Double): Double
  }

  abstract class ConnectorChargeable(val connector: Connector) extends Chargeable {
    override def changeBuffer(delta: Double): Double = connector.changeBuffer(delta)

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: ConnectorChargeable => chargeable.connector == connector
      case _ => false
    }
  }

  class RobotChargeable(val robot: Robot) extends ConnectorChargeable(robot.getNode.asInstanceOf[Connector]) {
    override def pos: Vec3d = BlockPosition(robot).toVec3

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: RobotChargeable => chargeable.robot == robot
      case _ => false
    }

    override def hashCode(): Int = robot.hashCode()
  }

  class DroneChargeable(val drone: Drone) extends ConnectorChargeable(drone.components.getNode.asInstanceOf[Connector]) {
    override def pos: Vec3d = new Vec3d(drone.posX, drone.posY, drone.posZ)

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: DroneChargeable => chargeable.drone == drone
      case _ => false
    }

    override def hashCode(): Int = drone.hashCode()
  }

  class PlayerChargeable(val player: EntityPlayer) extends Chargeable {
    override def pos: Vec3d = new Vec3d(player.posX, player.posY, player.posZ)

    override def changeBuffer(delta: Double): Double = {
      api.Nanomachines.getController(player) match {
        case controller: Controller => controller.changeBuffer(delta)
        case _ => delta // Cannot charge.
      }
    }

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: PlayerChargeable => chargeable.player == player
      case _ => false
    }

    override def hashCode(): Int = player.hashCode()
  }

}
