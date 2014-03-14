package li.cil.oc.common.tileentity

import buildcraft.api.power.{IPowerReceptor, PowerHandler}
//import cofh.api.energy.IEnergyHandler
import cpw.mods.fml.common.{ModAPIManager, Loader, Optional}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import ic2.api.energy.event.{EnergyTileUnloadEvent, EnergyTileLoadEvent}
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.api.network.Connector
import li.cil.oc.Settings
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection
//import universalelectricity.api.energy.{IEnergyContainer, IEnergyInterface}

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraftAPI|power"),
  new Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2")
//  new Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "ThermalExpansion"),
//  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyInterface", modid = "UniversalElectricity"),
//  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyContainer", modid = "UniversalElectricity")
))
abstract class PowerAcceptor extends TileEntity with IPowerReceptor with IEnergySink /* with IEnergyHandler with IEnergyInterface with IEnergyContainer */ {
  @SideOnly(Side.CLIENT)
  protected def hasConnector(side: ForgeDirection) = false

  protected def connector(side: ForgeDirection): Option[Connector] = None

  // ----------------------------------------------------------------------- //

  private lazy val isIndustrialCraftAvailable = Loader.isModLoaded("IC2")

  private lazy val isBuildCraftAvailable = ModAPIManager.INSTANCE.hasAPI("BuildCraftAPI|power")

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer && !Settings.get.ignorePower) {
      if (isBuildCraftAvailable && world.getWorldTime % Settings.get.tickFrequency == 0) {
        for (side <- ForgeDirection.VALID_DIRECTIONS) connector(side) match {
          case Some(node) =>
            val demand = node.globalBufferSize - node.globalBuffer
            if (demand > 1) {
              node.changeBuffer(getPowerProvider.useEnergy(1, demand.toFloat, true))
            }
          case _ =>
        }
      }
      if (isIndustrialCraftAvailable) {
        loadIC2()
      }
    }
  }

  override def invalidate() {
    super.invalidate()
    if (isIndustrialCraftAvailable) {
      unloadIC2()
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (isIndustrialCraftAvailable) {
      unloadIC2()
    }
  }

  // ----------------------------------------------------------------------- //
  // BuildCraft

  private var powerHandler: Option[AnyRef] = None

  @Optional.Method(modid = "BuildCraftAPI|power")
  def getPowerProvider = {
    if (powerHandler.isEmpty) {
      val handler = new PowerHandler(this, PowerHandler.Type.MACHINE)
      if (handler != null) {
        handler.configure(1, 320, Float.MaxValue, 640)
        handler.configurePowerPerdition(0, 0)
        powerHandler = Some(handler)
      }
    }
    if (powerHandler.isDefined) powerHandler.get.asInstanceOf[PowerHandler]
    else null
  }

  @Optional.Method(modid = "BuildCraftAPI|power")
  def getPowerReceiver(side: ForgeDirection) =
    if (!Settings.get.ignorePower && (if (isClient) hasConnector(side) else connector(side).isDefined))
      getPowerProvider.getPowerReceiver
    else null

  // Don't strip, also defined by AbstractBusAware trait.
  def getWorld = worldObj

  @Optional.Method(modid = "BuildCraftAPI|power")
  def doWork(workProvider: PowerHandler) {}

  // ----------------------------------------------------------------------- //
  // IndustrialCraft2

  private var addedToPowerGrid = false

  @Optional.Method(modid = "IC2")
  def loadIC2() {
    if (!addedToPowerGrid) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this))
      addedToPowerGrid = true
    }
  }

  @Optional.Method(modid = "IC2")
  def unloadIC2() {
    if (addedToPowerGrid) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this))
      addedToPowerGrid = false
    }
  }

  @Optional.Method(modid = "IC2")
  def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = "IC2")
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = canConnect(direction, null)

  @Optional.Method(modid = "IC2")
  def demandedEnergyUnits = {
    if (Settings.get.ignorePower || isClient) 0
    else {
      var maxDemand = 0.0
      for (side <- ForgeDirection.VALID_DIRECTIONS) connector(side) match {
        case Some(node) =>
          maxDemand = math.max(maxDemand, node.globalBufferSize - node.globalBuffer)
        case _ =>
      }
      maxDemand * Settings.ratioBC / Settings.ratioIC2
    }
  }

  @Optional.Method(modid = "IC2")
  def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double) =
    amount - onReceiveEnergy(directionFrom, (amount * Settings.ratioIC2).toLong, doReceive = true) / Settings.ratioIC2

  // ----------------------------------------------------------------------- //
  // Thermal Expansion

  @Optional.Method(modid = "ThermalExpansion")
  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
    (onReceiveEnergy(from, (maxReceive * Settings.ratioTE).toLong, !simulate) / Settings.ratioTE).toInt

  @Optional.Method(modid = "ThermalExpansion")
  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) =
    (onExtractEnergy(from, (maxExtract * Settings.ratioTE).toLong, !simulate) / Settings.ratioTE).toInt

  @Optional.Method(modid = "ThermalExpansion")
  def canInterface(from: ForgeDirection) = canConnect(from, this)

  @Optional.Method(modid = "ThermalExpansion")
  def getEnergyStored(from: ForgeDirection) = (getEnergy(from) / Settings.ratioTE).toInt

  @Optional.Method(modid = "ThermalExpansion")
  def getMaxEnergyStored(from: ForgeDirection) = (getEnergyCapacity(from) / Settings.ratioTE).toInt

  // ----------------------------------------------------------------------- //
  // Universal Electricity

  def canConnect(direction: ForgeDirection, source: AnyRef) =
    !Settings.get.ignorePower && direction != null && direction != ForgeDirection.UNKNOWN &&
      (if (isClient) hasConnector(direction) else connector(direction).isDefined)

  def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) =
    if (isClient || Settings.get.ignorePower) 0
    else connector(from) match {
      case Some(node) =>
        val energy = receive / Settings.ratioBC
        if (doReceive) {
          val surplus = node.changeBuffer(energy)
          (receive - surplus * Settings.ratioBC).toLong
        }
        else {
          val space = node.globalBufferSize - node.globalBuffer
          math.min(receive, space * Settings.ratioBC).toLong
        }
      case _ => 0
    }

  def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  def setEnergy(from: ForgeDirection, energy: Long) {}

  def getEnergy(from: ForgeDirection) =
    if (isClient) 0
    else connector(from) match {
      case Some(node) => (node.globalBuffer * Settings.ratioBC).toLong
      case _ => 0
    }

  def getEnergyCapacity(from: ForgeDirection) =
    if (isClient) 0
    else connector(from) match {
      case Some(node) => (node.globalBufferSize * Settings.ratioBC).toLong
      case _ => 0
    }
}
