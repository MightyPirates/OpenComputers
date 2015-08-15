package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.internal
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

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Charger extends traits.Environment with traits.PowerAcceptor with traits.RedstoneAware with traits.Rotatable with traits.ComponentInventory with Analyzable with traits.StateAware {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  val connectors = mutable.Set.empty[(Vec3, Connector)]

  var chargeSpeed = 0.0

  var hasPower = false

  var invertSignal = false

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) node else null)

  override def energyThroughput = Settings.get.chargerRate

  override def getCurrentState = {
    // TODO Refine to only report working if present robots/drones actually *need* power.
    if (connectors.nonEmpty) {
      if (hasPower) util.EnumSet.of(internal.StateAware.State.IsWorking)
      else util.EnumSet.of(internal.StateAware.State.CanWork)
    }
    else util.EnumSet.noneOf(classOf[internal.StateAware.State])
  }

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.addChatMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed))
    null
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = true

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
          connectors.foreach {
            case (_, connector) => node.changeBuffer(connector.changeBuffer(charge + node.changeBuffer(-charge)))
          }
        }
      }

      // Charging of internal devices.
      {
        val charge = Settings.get.chargeRateTablet * chargeSpeed * Settings.get.tickFrequency
        canCharge ||= charge > 0 && node.globalBuffer >= charge * 0.5
        if (canCharge) {
          (0 until getSizeInventory).map(getStackInSlot).foreach(stack => if (stack != null) {
            val offered = charge + node.changeBuffer(-charge)
            val surplus = ItemCharge.charge(stack, offered)
            node.changeBuffer(surplus)
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

    if (isClient && chargeSpeed > 0 && hasPower && world.getWorldInfo.getWorldTotalTime % 10 == 0) {
      connectors.foreach {
        case (position, _) =>
          val theta = world.rand.nextDouble * Math.PI
          val phi = world.rand.nextDouble * Math.PI * 2
          val dx = 0.45 * Math.sin(theta) * Math.cos(phi)
          val dy = 0.45 * Math.sin(theta) * Math.sin(phi)
          val dz = 0.45 * Math.cos(theta)
          world.spawnParticle("happyVillager", position.xCoord + dx, position.yCoord + dz, position.zCoord + dy, 0, 0, 0)
      }
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
    val signal = math.max(0, math.min(15, ForgeDirection.VALID_DIRECTIONS.map(input).max))

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
    val robotConnectors = ForgeDirection.VALID_DIRECTIONS.map(side => {
      val blockPos = BlockPosition(this).offset(side)
      if (world.blockExists(blockPos)) Option(world.getTileEntity(blockPos))
      else None
    }).collect {
      case Some(t: RobotProxy) => (BlockPosition(t).toVec3, t.robot.node.asInstanceOf[Connector])
    }
    val droneConnectors = world.getEntitiesWithinAABB(classOf[Drone], BlockPosition(this).bounds.expand(1, 1, 1)).collect {
      case drone: Drone => (Vec3.createVectorHelper(drone.posX, drone.posY, drone.posZ), drone.components.node.asInstanceOf[Connector])
    }

    // Only update list when we have to, keeps pointless block updates to a minimum.
    if (connectors.size != robotConnectors.length + droneConnectors.size || (connectors.size > 0 && connectors.map(_._2).diff((robotConnectors ++ droneConnectors).map(_._2).toSet).size > 0)) {
      connectors.clear()
      connectors ++= robotConnectors
      connectors ++= droneConnectors
      world.notifyBlocksOfNeighborChange(x, y, z, block)
    }
  }
}
