package li.cil.oc.common.tileentity

import buildcraft.api.power.{PowerHandler, IPowerReceptor}
import cpw.mods.fml.common.{Loader, Optional}
import ic2.api.energy.event.{EnergyTileLoadEvent, EnergyTileUnloadEvent}
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.{Config, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.{ForgeDirection, MinecraftForge}
import universalelectricity.core.block.IElectrical
import universalelectricity.core.electricity.ElectricityPack

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
  new Optional.Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraft|Energy")))
class PowerConverter extends Rotatable with Environment with IEnergySink with IPowerReceptor with IElectrical {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(Config.bufferConverter).
    create()

  private def demand = node.bufferSize - node.buffer

  // ----------------------------------------------------------------------- //
  // Energy conversion ratios, Mode -> Internal

  val ratioIndustrialCraft = 2

  val ratioBuildCraft = 5

  val ratioUniversalElectricity = 5

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (node != null && node.network == null) {
      Network.joinOrCreateNetwork(worldObj, xCoord, yCoord, zCoord)
    }
    if (!worldObj.isRemote) {
      if (Loader.isModLoaded("IC2")) {
        loadIC2()
      }
      if (demand > 0 && Loader.isModLoaded("BuildCraft|Energy")) {
        node.changeBuffer(getPowerProvider.useEnergy(1, demand.toFloat / ratioBuildCraft, true) * ratioBuildCraft)
      }
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    unload()
  }

  def unload() {
    if (Loader.isModLoaded("IC2")) {
      unloadIC2()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (node != null) node.load(nbt)

    if (Loader.isModLoaded("BuildCraft|Energy"))
      getPowerProvider.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (node != null) node.save(nbt)

    if (Loader.isModLoaded("BuildCraft|Energy"))
      getPowerProvider.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //
  // IndustrialCraft

  private var isIC2Loaded = false

  private var lastPacketSize = 0.0

  @Optional.Method(modid = "IC2")
  def loadIC2() {
    if (!isIC2Loaded) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this))
      isIC2Loaded = true
    }
  }

  @Optional.Method(modid = "IC2")
  def unloadIC2() {
    if (isIC2Loaded) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this))
      isIC2Loaded = false
    }
  }

  @Optional.Method(modid = "IC2")
  override def acceptsEnergyFrom(emitter: TileEntity, direction: ForgeDirection) = true

  @Optional.Method(modid = "IC2")
  override def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = "IC2")
  override def demandedEnergyUnits = {
    // We try to avoid requesting energy when we need less than what we get with
    // a single packet. However, if our buffer gets dangerously low we will ask
    // for energy even if there's the danger of wasting some energy.
    if (demand >= lastPacketSize * ratioIndustrialCraft || demand > node.bufferSize * 0.5) {
      demand
    } else 0
  }

  @Optional.Method(modid = "IC2")
  override def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double) = {
    lastPacketSize = amount
    node.changeBuffer(amount * ratioIndustrialCraft)
    0
  }

  // ----------------------------------------------------------------------- //
  // BuildCraft

  private var powerHandler: Option[AnyRef] = None

  @Optional.Method(modid = "BuildCraft|Energy")
  def getPowerProvider = {
    if (node != null && powerHandler.isEmpty) {
      val handler = new PowerHandler(this, PowerHandler.Type.STORAGE)
      if (handler != null) {
        handler.configure(1, 320, Float.MaxValue, node.bufferSize.toFloat / ratioBuildCraft)
        handler.configurePowerPerdition(0, 0)
        powerHandler = Some(handler)
      }
    }
    powerHandler.fold(null: PowerHandler)(_.asInstanceOf[PowerHandler])
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
  // Universal Electricity

  def canConnect(direction: ForgeDirection) = true

  def getVoltage = 120f

  def getRequest(direction: ForgeDirection) = demand.toFloat / ratioUniversalElectricity

  def receiveElectricity(from: ForgeDirection, receive: ElectricityPack, doReceive: Boolean) = {
    if (receive != null) {
      if (doReceive) {
        node.changeBuffer(receive.getWatts * ratioUniversalElectricity)
      }
      receive.getWatts
    } else 0
  }

  def getProvide(direction: ForgeDirection) = 0f

  def provideElectricity(from: ForgeDirection, request: ElectricityPack, doProvide: Boolean) = null
}
