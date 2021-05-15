package li.cil.oc.common.tileentity.traits.power
/* TODO IC2 Classic

import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.eventhandler.Event
import ic2classic.api.Direction
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection

@Injectable.Interface(value = "ic2classic.api.energy.tile.IEnergySink", modid = Mods.IDs.IndustrialCraft2Classic)
trait IndustrialCraft2Classic extends Common with IndustrialCraft2Common {
  private var conversionBuffer = 0.0

  private lazy val useIndustrialCraft2ClassicPower = isServer && Mods.IndustrialCraft2Classic.isAvailable

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useIndustrialCraft2ClassicPower && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  private def updateEnergy() {
    tryAllSides((demand, _) => {
      val result = math.min(demand, conversionBuffer)
      conversionBuffer -= result
      result
    }, Power.fromEU, Power.toEU)
  }

  override def validate() {
    super.validate()
    if (useIndustrialCraft2ClassicPower && !addedToIC2PowerGrid) EventHandler.scheduleIC2Add(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useIndustrialCraft2ClassicPower && addedToIC2PowerGrid) removeFromIC2Grid()
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useIndustrialCraft2ClassicPower && addedToIC2PowerGrid) removeFromIC2Grid()
  }

  private def removeFromIC2Grid() {
    try MinecraftForge.EVENT_BUS.post(Class.forName("ic2classic.api.energy.event.EnergyTileUnloadEvent").getConstructor(Class.forName("ic2classic.api.energy.tile.IEnergyTile")).newInstance(this).asInstanceOf[Event]) catch {
      case t: Throwable => OpenComputers.log.warn("Error removing node from IC2 grid.", t)
    }
    addedToIC2PowerGrid = false
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    conversionBuffer = nbt.getDouble(Settings.namespace + "ic2cpower")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setDouble(Settings.namespace + "ic2cpower", conversionBuffer)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def isAddedToEnergyNet: Boolean = addedToIC2PowerGrid

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def getMaxSafeInput: Int = Int.MaxValue

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def acceptsEnergyFrom(emitter: TileEntity, direction: Direction) = useIndustrialCraft2ClassicPower && canConnectPower(direction.toForgeDirection)

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def injectEnergy(directionFrom: Direction, amount: Int): Boolean = {
    conversionBuffer += amount
    true
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def demandsEnergy: Int = {
    if (!useIndustrialCraft2ClassicPower) 0
    else if (conversionBuffer < energyThroughput * Settings.get.tickFrequency)
      math.min(ForgeDirection.VALID_DIRECTIONS.map(globalDemand).max, Power.toEU(energyThroughput)).toInt
    else 0
  }
}
*/