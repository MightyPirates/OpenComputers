package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.network._
import li.cil.oc.common.Slot
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.Vec3
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

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
  override protected def hasConnector(side: EnumFacing) = side != facing

  override protected def connector(side: EnumFacing) = Option(if (side != facing) node else null)

  override protected def energyThroughput = Settings.get.chargerRate

  override def currentState = {
    // TODO Refine to only report working if present robots/drones actually *need* power.
    if (connectors.nonEmpty) {
      if (hasPower) util.EnumSet.of(traits.State.IsWorking)
      else util.EnumSet.of(traits.State.CanWork)
    }
    else util.EnumSet.noneOf(classOf[traits.State])
  }

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
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
      val charge = Settings.get.chargeRateExternal * chargeSpeed * Settings.get.tickFrequency
      val canCharge = charge > 0 && (node.globalBuffer >= charge * 0.5 || Settings.get.ignorePower)
      if (hasPower && !canCharge) {
        hasPower = false
        ServerPacketSender.sendChargerState(this)
      }
      if (!hasPower && canCharge) {
        hasPower = true
        ServerPacketSender.sendChargerState(this)
      }
      if (canCharge) {
        connectors.foreach {
          case (_, connector) => node.changeBuffer(connector.changeBuffer(charge + node.changeBuffer(-charge)))
        }
      }

      // Charge tablet if present.
      val stack = getStackInSlot(0)
      if (stack != null && chargeSpeed > 0) {
        def tryCharge(energy: Double, maxEnergy: Double, handler: (Double) => Unit) {
          if (energy < maxEnergy) {
            val itemCharge = math.min(maxEnergy - energy, Settings.get.chargeRateTablet * chargeSpeed * Settings.get.tickFrequency)
            if (node.tryChangeBuffer(-itemCharge))
              handler(itemCharge)
          }
        }
        val data = new TabletData(stack)
        tryCharge(data.energy, data.maxEnergy, (amount) => {
          data.energy = math.min(data.maxEnergy, data.energy + amount)
          data.save(stack)
        })
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
          world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, position.xCoord + dx, position.yCoord + dz, position.zCoord + dy, 0, 0, 0)
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

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Tablet
    case _ => false
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    Tablet.Server.cache.invalidate(Tablet.getId(stack))
    components(slot) match {
      case Some(environment) => environment.node match {
        case component: Component => component.setVisibility(Visibility.Network)
      }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def updateRedstoneInput(side: EnumFacing) {
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
    val robotConnectors = EnumFacing.values.map(side => {
      val blockPos = BlockPosition(this).offset(side)
      if (world.blockExists(blockPos)) Option(world.getTileEntity(blockPos))
      else None
    }).collect {
      case Some(t: RobotProxy) => (BlockPosition(t).toVec3, t.robot.node.asInstanceOf[Connector])
    }
    val droneConnectors = world.getEntitiesWithinAABB(classOf[Drone], BlockPosition(this).bounds.expand(1, 1, 1)).collect {
      case drone: Drone => (new Vec3(drone.posX, drone.posY, drone.posZ), drone.components.node.asInstanceOf[Connector])
    }

    // Only update list when we have to, keeps pointless block updates to a minimum.
    if (connectors.size != robotConnectors.size + droneConnectors.size || (connectors.size > 0 && connectors.map(_._2).diff((robotConnectors ++ droneConnectors).map(_._2).toSet).size > 0)) {
      connectors.clear()
      connectors ++= robotConnectors
      connectors ++= droneConnectors
      world.notifyNeighborsOfStateChange(getPos, getBlockType)
    }
  }
}
