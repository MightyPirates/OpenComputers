package li.cil.oc.common.tileentity

import net.minecraft.tileentity.TileEntity
import li.cil.oc.api.network.{PoweredNode, Visibility}
import net.minecraftforge.common.{ForgeDirection, MinecraftForge}
import ic2.api.energy.event.{EnergyTileLoadEvent, EnergyTileUnloadEvent}
import cpw.mods.fml.common.FMLCommonHandler
import ic2.api.energy.tile.IEnergySink
import buildcraft.api.power.{PowerHandler, IPowerReceptor}
import net.minecraft.world.World
import net.minecraft.nbt.NBTTagCompound
import universalelectricity.core.block.IElectrical
import universalelectricity.core.electricity.ElectricityPack

/**
 * Created with IntelliJ IDEA.
 * User: lordjoda
 * Date: 30.09.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
class PowerSupply extends Rotatable with PoweredNode with IEnergySink with IPowerReceptor with IElectrical {
  var addedToEnet = false
  var powerHandler: PowerHandler = null

  override def name = "powersupply"

  override def visibility = Visibility.Network

  override def onChunkUnload() {
    super.onChunkUnload()
    onUnload()
  }

  def onUnload() {
    if (addedToEnet) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this))
      addedToEnet = false
    }

  }

  override def updateEntity() {
    super.updateEntity()
    if (!addedToEnet) {
      onLoaded()
    }
    if (!FMLCommonHandler.instance.getEffectiveSide.isClient) {
      main.addEnergy((getPowerProvider().useEnergy(1, main.getDemand.toFloat / 5.0f, true) * 5).toInt)

    }
  }

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    getPowerProvider().readFromNBT(nbt)

  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    getPowerProvider().writeToNBT(nbt)


  }

  /**
   * Notification that the TileEntity finished loaded, for advanced uses.
   * Either onUpdateEntity or onLoaded have to be used.
   */
  def onLoaded() {
    if (!addedToEnet && !FMLCommonHandler.instance.getEffectiveSide.isClient) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this))
      addedToEnet = true
    }
  }


  var lastInjectedEnergy = 0.0
  //IC2 stuff
  /**
   * Determine how much energy the sink accepts.
   *
   * This value is unrelated to getMaxSafeInput().
   *
   * Make sure that injectEnergy() does accepts energy if demandsEnergy() returns anything > 0.
   *
   * @return max accepted input in eu
   */
  override def demandedEnergyUnits: Double = {
    val needed = main.getDemand
    if (needed > lastInjectedEnergy || needed > main.MAXENERGY / 2)
      return needed / 2
    0
  }

  /**
   * Transfer energy to the sink.
   *
   * It's highly recommended to accept all energy by letting the internal buffer overflow to
   * increase the performance and accuracy of the distribution simulation.
   *
   * @param directionFrom direction from which the energy comes from
   * @param amount energy to be transferred
   * @return Energy not consumed (leftover)
   */
  override def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double): Double = {
    lastInjectedEnergy = amount * 2.0
    main.addEnergy((amount*2.0).toInt)
    0
  }

  /**
   * Determine the amount of eu which can be safely injected into the specific energy sink without exploding.
   *
   * Typical values are 32 for LV, 128 for MV, 512 for HV and 2048 for EV. A value of Integer.MAX_VALUE indicates no
   * limit.
   *
   * This value is unrelated to demandsEnergy().
   *
   * @return max safe input in eu
   */
  override def getMaxSafeInput: Int = Integer.MAX_VALUE

  /**
   * Determine if this acceptor can accept current from an adjacent emitter in a direction.
   *
   * The TileEntity in the emitter parameter is what was originally added to the energy net,
   * which may be normal in-world TileEntity, a delegate or an IMetaDelegate.
   *
   * @param emitter energy emitter
   * @param direction direction the energy is being received from
   */
  override def acceptsEnergyFrom(emitter: TileEntity, direction: ForgeDirection): Boolean = true

  //*******************BUILDCRAFT**********************************//


  /**
   * Get the PowerReceiver for this side of the block. You can return the same PowerReceiver for
   * all sides or one for each side.
   *
   * You should NOT return null to this method unless you mean to NEVER receive power from that
   * side. Returning null, after previous returning a PowerReceiver, will most likely cause pipe
   * connections to derp out and engines to eventually explode.
   *
   * @param side
   * @return
   */
  def getPowerReceiver(side: ForgeDirection): PowerHandler#PowerReceiver = {

    getPowerProvider().getPowerReceiver
  }

  def getPowerProvider(): PowerHandler = {
    if (powerHandler == null) {
      powerHandler = new PowerHandler(this, PowerHandler.Type.STORAGE);
      if (powerHandler != null) {
        powerHandler.configure(1.0F, 320.0F, 800.0F, 640.0F);
      }
    }
    powerHandler;
  }

  /**
   * Call back from the PowerHandler that is called when the stored power exceeds the activation
   * power.
   *
   * It can be triggered by update() calls or power modification calls.
   *
   * @param workProvider
   */
  def doWork(workProvider: PowerHandler) {

  }

  def getWorld: World = worldObj


  /** * UE*************************
    *
    */
  /**
   * Adds electricity to an block. Returns the quantity of electricity that was accepted. This
   * should always return 0 if the block cannot be externally charged.
   *
   * @param from Orientation the electricity is sent in from.
   * @param receive Maximum amount of electricity to be sent into the block.
   * @param doReceive If false, the charge will only be simulated.
   * @return Amount of energy that was accepted by the block.
   */
  def receiveElectricity(from: ForgeDirection, receive: ElectricityPack, doReceive: Boolean): Float = {
    if (receive == null) return 0.0F

    if (doReceive) {
      val energy = receive.getWatts() / 0.2F
      main.addEnergy(energy.toInt)
    }
    receive.getWatts()
  }

  /**
   * Adds electricity to an block. Returns the ElectricityPack, the electricity provided. This
   * should always return null if the block cannot be externally discharged.
   *
   * @param from Orientation the electricity is requested from.
   * @param request Maximum amount of energy to be sent into the block.
   * @param doProvide If false, the charge will only be simulated.
   * @return Amount of energy that was given out by the block.
   */
  def provideElectricity(from: ForgeDirection, request: ElectricityPack, doProvide: Boolean): ElectricityPack = null

  /**
   * @return How much energy does this TileEntity want?
   */
  def getRequest(direction: ForgeDirection): Float = {
    val diff = Math.floor(main.getDemand * 0.2F)
    diff.toFloat max 0
  }

  /**
   * @return How much energy does this TileEntity want to provide?
   */
  def getProvide(direction: ForgeDirection): Float = 0.0F

  /**
   * Gets the voltage of this TileEntity.
   *
   * @return The amount of volts. E.g 120v or 240v
   */
  def getVoltage: Float = 120.0F

  def canConnect(direction: ForgeDirection): Boolean = true

}
