package li.cil.oc.common.tileentity

import buildcraft.api.power.{PowerHandler, IPowerReceptor}
import ic2.api.energy.event.{EnergyTileLoadEvent, EnergyTileUnloadEvent}
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.{ForgeDirection, MinecraftForge}
import universalelectricity.core.block.IElectrical
import universalelectricity.core.electricity.ElectricityPack

class PowerConverter extends Rotatable with Environment with IEnergySink with IPowerReceptor with IElectrical {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(128).
    create()

  private var addedToEnet = false

  private var lastPacketSize = 0.0

  private var powerHandler: PowerHandler = null

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
      if (!addedToEnet) {
        MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this))
        addedToEnet = true
      }
      if (demand > 0) {
        node.changeBuffer(getPowerProvider.useEnergy(1, demand.toFloat / ratioBuildCraft, true) * ratioBuildCraft)
      }
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    unload()
  }

  def unload() {
    if (addedToEnet) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this))
      addedToEnet = false
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    node.load(nbt)
    getPowerProvider.readFromNBT(nbt)

  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    node.save(nbt)
    getPowerProvider.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //
  // IndustrialCraft

  override def acceptsEnergyFrom(emitter: TileEntity, direction: ForgeDirection) = true

  override def getMaxSafeInput = Integer.MAX_VALUE

  override def demandedEnergyUnits = {
    // We try to avoid requesting energy when we need less than what we get with
    // a single packet. However, if our buffer gets dangerously low we will ask
    // for energy even if there's the danger of wasting some energy.
    if (demand >= lastPacketSize * ratioIndustrialCraft || demand > node.bufferSize * 0.5) {
      demand
    } else 0
  }

  override def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double) = {
    lastPacketSize = amount
    node.changeBuffer(amount * ratioIndustrialCraft)
    0
  }

  // ----------------------------------------------------------------------- //
  // BuildCraft

  def getPowerProvider = {
    if (powerHandler == null) {
      powerHandler = new PowerHandler(this, PowerHandler.Type.STORAGE)
      if (powerHandler != null) {
        powerHandler.configure(1, 320, Float.MaxValue, node.bufferSize.toFloat / ratioBuildCraft)
      }
    }
    powerHandler
  }

  def getPowerReceiver(side: ForgeDirection) = getPowerProvider.getPowerReceiver

  def getWorld = worldObj

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
