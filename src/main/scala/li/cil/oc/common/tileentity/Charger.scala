package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
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
import li.cil.oc.integration.util.ItemCharge
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.Vec3
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Charger extends traits.Environment with traits.PowerAcceptor with traits.RedstoneAware with traits.Rotatable with traits.ComponentInventory with Analyzable with traits.StateAware with DeviceInfo {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  val connectors = mutable.Set.empty[Chargeable]
  val equipment = mutable.Set.empty[ItemStack]

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
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) node else null)

  override def energyThroughput = Settings.get.chargerRate

  override def getCurrentState = {
    // TODO Refine to only report working if present robots/drones actually *need* power.
    if (connectors.nonEmpty) {
      if (hasPower) util.EnumSet.of(api.util.StateAware.State.IsWorking)
      else util.EnumSet.of(api.util.StateAware.State.CanWork)
    }
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.addChatMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed))
    null
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = true

  private def chargeStack(stack: ItemStack, charge: Double): Unit = {
    if (stack != null && charge > 0) {
      val offered = charge + node.changeBuffer(-charge)
      val surplus = ItemCharge.charge(stack, offered)
      node.changeBuffer(surplus)
    }
  }

  override def updateEntity() {
    super.updateEntity()

    // Offset by hashcode to avoid all chargers ticking at the same time.
    if ((world.getWorldInfo.getWorldTotalTime + math.abs(hashCode())) % 20 == 0) {
      updateConnectors()
    }

    if (isServer && world.getWorldInfo.getWorldTotalTime % Settings.get.tickFrequency == 0) {
      var canCharge = Settings.get.ignorePower

      // Charging of external devices.
      {
        val charge = Settings.get.chargeRateExternal * chargeSpeed * Settings.get.tickFrequency
        canCharge ||= charge > 0 && node.globalBuffer >= charge * 0.5
        if (canCharge) {
          connectors.foreach(connector => node.changeBuffer(connector.changeBuffer(charge + node.changeBuffer(-charge))))
        }
      }

      // Charging of internal devices.
      {
        val charge = Settings.get.chargeRateTablet * chargeSpeed * Settings.get.tickFrequency
        canCharge ||= charge > 0 && node.globalBuffer >= charge * 0.5
        if (canCharge) {
          (0 until getSizeInventory).map(getStackInSlot).foreach(chargeStack(_, charge))
        }
      }

      // Charging of equipment
      {
        val charge = Settings.get.chargeRateTablet * chargeSpeed * Settings.get.tickFrequency
        canCharge ||= charge > 0 && node.globalBuffer >= charge * 0.5
        if (canCharge) {
          equipment.foreach(chargeStack(_, charge))
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

    if (isClient && chargeSpeed > 0 && hasPower && world.getWorldInfo.getWorldTotalTime % 10 == 0) {
      connectors.foreach(connector => {
        val position = connector.pos
        val theta = world.rand.nextDouble * Math.PI
        val phi = world.rand.nextDouble * Math.PI * 2
        val dx = 0.45 * Math.sin(theta) * Math.cos(phi)
        val dy = 0.45 * Math.sin(theta) * Math.sin(phi)
        val dz = 0.45 * Math.cos(theta)
        world.spawnParticle("happyVillager", position.xCoord + dx, position.yCoord + dz, position.zCoord + dy, 0, 0, 0)
      })
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      onNeighborChanged()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    chargeSpeed = nbt.getDouble("chargeSpeed") max 0 min 1
    hasPower = nbt.getBoolean("hasPower")
    invertSignal = nbt.getBoolean("invertSignal")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setDouble("chargeSpeed", chargeSpeed)
    nbt.setBoolean("hasPower", hasPower)
    nbt.setBoolean("invertSignal", invertSignal)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    chargeSpeed = nbt.getDouble("chargeSpeed")
    hasPower = nbt.getBoolean("hasPower")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble("chargeSpeed", chargeSpeed)
    nbt.setBoolean("hasPower", hasPower)
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

  override def updateRedstoneInput(side: ForgeDirection) {
    super.updateRedstoneInput(side)
    val signal = getInput.max min 15

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
    val robots = ForgeDirection.VALID_DIRECTIONS.map(side => {
      val blockPos = BlockPosition(this).offset(side)
      if (world.blockExists(blockPos)) Option(world.getTileEntity(blockPos))
      else None
    }).collect {
      case Some(t: RobotProxy) => new RobotChargeable(t.robot)
    }

    val bounds = BlockPosition(this).bounds.expand(1, 1, 1)
    val drones = world.getEntitiesWithinAABB(classOf[Drone], bounds).collect {
      case drone: Drone => new DroneChargeable(drone)
    }

    val players = world.getEntitiesWithinAABB(classOf[EntityPlayer], bounds).collect {
      case player: EntityPlayer => player
    }

    val chargeablePlayers = players.collect {
      case player if api.Nanomachines.hasController(player) => new PlayerChargeable(player)
    }

    // Only update list when we have to, keeps pointless block updates to a minimum.

    val newConnectors = robots ++ drones ++ chargeablePlayers
    if (connectors.size != newConnectors.length || (connectors.nonEmpty && (connectors -- newConnectors).nonEmpty)) {
      connectors.clear()
      connectors ++= newConnectors
      world.notifyBlocksOfNeighborChange(x, y, z, block)
    }

    // scan players for chargeable equipment
    equipment.clear()
    players.foreach {
      player => player.inventory.mainInventory.foreach {
        stack: ItemStack =>
          if (Option(Driver.driverFor(stack, getClass)) match {
            case Some(driver) if driver.slot(stack) == Slot.Tablet => true
            case _ => ItemCharge.canCharge(stack)
          }) {
            equipment += stack
          }
      }
    }
  }

  trait Chargeable {
    def pos: Vec3

    def changeBuffer(delta: Double): Double
  }

  abstract class ConnectorChargeable(val connector: Connector) extends Chargeable {
    override def changeBuffer(delta: Double): Double = connector.changeBuffer(delta)

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: ConnectorChargeable => chargeable.connector == connector
      case _ => false
    }
  }

  class RobotChargeable(val robot: Robot) extends ConnectorChargeable(robot.node.asInstanceOf[Connector]) {
    override def pos: Vec3 = BlockPosition(robot).toVec3

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: RobotChargeable => chargeable.robot == robot
      case _ => false
    }

    override def hashCode(): Int = robot.hashCode()
  }

  class DroneChargeable(val drone: Drone) extends ConnectorChargeable(drone.components.node.asInstanceOf[Connector]) {
    override def pos: Vec3 = Vec3.createVectorHelper(drone.posX, drone.posY, drone.posZ)

    override def equals(obj: scala.Any): Boolean = obj match {
      case chargeable: DroneChargeable => chargeable.drone == drone
      case _ => false
    }

    override def hashCode(): Int = drone.hashCode()
  }

  class PlayerChargeable(val player: EntityPlayer) extends Chargeable {
    override def pos: Vec3 = Vec3.createVectorHelper(player.posX, player.posY, player.posZ)

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
