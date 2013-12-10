package li.cil.oc.common.tileentity

import buildcraft.api.power.{PowerHandler, IPowerReceptor}
import cofh.api.energy.IEnergyHandler
import cpw.mods.fml.common.{Loader, Optional}
import ic2.api.energy.event.{EnergyTileLoadEvent, EnergyTileUnloadEvent}
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.{ForgeDirection, MinecraftForge}
import universalelectricity.core.block.IElectrical
import universalelectricity.core.electricity.ElectricityPack

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
  new Optional.Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraft|Energy"),
  new Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "ThermalExpansion")
))
class PowerConverter extends Environment with Analyzable with IEnergySink with IPowerReceptor with IElectrical with IEnergyHandler {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector().
    create()

  private lazy val isIndustrialCraftAvailable = Loader.isModLoaded("IC2")

  private lazy val isBuildCraftAvailable = Loader.isModLoaded("BuildCraft|Energy")

  private def demand = if (Settings.get.ignorePower) 0.0 else node.globalBufferSize - node.globalBuffer

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      if (isIndustrialCraftAvailable) {
        loadIC2()
      }
      if (isBuildCraftAvailable && demand > 1 && world.getWorldTime % Settings.get.tickFrequency == 0) {
        val wantInMJ = demand.toFloat / Settings.get.ratioBuildCraft
        val gotInMJ = getPowerProvider.useEnergy(1, wantInMJ, true)
        node.changeBuffer(gotInMJ * Settings.get.ratioBuildCraft)
      }
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (Loader.isModLoaded("IC2")) {
      unloadIC2()
    }
  }

  override def invalidate() {
    super.invalidate()
    if (Loader.isModLoaded("IC2")) {
      unloadIC2()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (Loader.isModLoaded("BuildCraft|Energy")) {
      getPowerProvider.readFromNBT(nbt.getCompoundTag(Settings.namespace + "bc"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (Loader.isModLoaded("BuildCraft|Energy")) {
      nbt.setNewCompoundTag(Settings.namespace + "bc", getPowerProvider.writeToNBT)
    }
  }

  // ----------------------------------------------------------------------- //
  // IndustrialCraft

  private var addedToPowerGrid = false

  private var lastPacketSize = 0.0

  private val maxPacketSize = 4096 * Settings.get.ratioIndustrialCraft2

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
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = !Settings.get.ignorePower

  @Optional.Method(modid = "IC2")
  def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = "IC2")
  def demandedEnergyUnits = {
    // We try to avoid requesting energy when we need less than what we get with
    // a single packet. However, if our buffer gets dangerously low we will ask
    // for energy even if there's the danger of wasting some energy.
    if (Settings.get.ignorePower || demand < lastPacketSize && demand < maxPacketSize) 0
    else demand / Settings.get.ratioIndustrialCraft2
  }

  @Optional.Method(modid = "IC2")
  def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double) = {
    if (Settings.get.ignorePower) amount
    else {
      lastPacketSize = amount * Settings.get.ratioIndustrialCraft2
      node.changeBuffer(lastPacketSize)
      0
    }
  }

  // ----------------------------------------------------------------------- //
  // BuildCraft

  private var powerHandler: Option[AnyRef] = None

  @Optional.Method(modid = "BuildCraft|Energy")
  def getPowerProvider = {
    if (node != null && powerHandler.isEmpty) {
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

  @Optional.Method(modid = "BuildCraft|Energy")
  def getPowerReceiver(side: ForgeDirection) =
    if (node != null)
      getPowerProvider.getPowerReceiver
    else null

  @Optional.Method(modid = "BuildCraft|Energy")
  def getWorld = worldObj

  @Optional.Method(modid = "BuildCraft|Energy")
  def doWork(workProvider: PowerHandler) {}

  // ----------------------------------------------------------------------- //
  // Thermal Expansion

  @Optional.Method(modid = "ThermalExpansion")
  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) = {
    if (Settings.get.ignorePower) 0
    else if (simulate) {
      val free = node.globalBufferSize - node.globalBuffer
      math.min(math.ceil(free / Settings.get.ratioThermalExpansion).toInt, maxReceive)
    }
    else (maxReceive - node.changeBuffer(maxReceive * Settings.get.ratioThermalExpansion) / Settings.get.ratioThermalExpansion).toInt
  }

  @Optional.Method(modid = "ThermalExpansion")
  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) = 0

  @Optional.Method(modid = "ThermalExpansion")
  def canInterface(from: ForgeDirection) = true

  @Optional.Method(modid = "ThermalExpansion")
  def getEnergyStored(from: ForgeDirection) = (node.globalBuffer / Settings.get.ratioThermalExpansion).toInt

  @Optional.Method(modid = "ThermalExpansion")
  def getMaxEnergyStored(from: ForgeDirection) = (node.globalBufferSize / Settings.get.ratioThermalExpansion).toInt

  // ----------------------------------------------------------------------- //
  // Universal Electricity

  def canConnect(direction: ForgeDirection) = !Settings.get.ignorePower

  def getVoltage = 120f

  def getRequest(direction: ForgeDirection) = {
    if (Settings.get.ignorePower) 0
    else demand.toFloat / Settings.get.ratioUniversalElectricity
  }

  def receiveElectricity(from: ForgeDirection, receive: ElectricityPack, doReceive: Boolean) = {
    if (receive != null) {
      if (doReceive) {
        node.changeBuffer(receive.getWatts * Settings.get.ratioUniversalElectricity)
      }
      receive.getWatts
    } else 0
  }

  def getProvide(direction: ForgeDirection) = 0f

  def provideElectricity(from: ForgeDirection, request: ElectricityPack, doProvide: Boolean) = null
}
