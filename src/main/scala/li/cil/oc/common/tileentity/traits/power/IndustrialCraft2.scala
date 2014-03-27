package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.{Loader, Optional}
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.common.EventHandler
import li.cil.oc.Settings
import net.minecraftforge.common.util.ForgeDirection

@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2")
trait IndustrialCraft2 extends Common with IEnergySink {
  var addedToPowerGrid = false

  private val useIndustrialCraft2Power = isServer && !Settings.get.ignorePower && Loader.isModLoaded("IC2")

  // ----------------------------------------------------------------------- //

  override def validate() {
    super.validate()
    if (useIndustrialCraft2Power && !addedToPowerGrid) EventHandler.scheduleIC2Add(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useIndustrialCraft2Power && addedToPowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useIndustrialCraft2Power && addedToPowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "IC2")
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = canConnect(direction)

  @Optional.Method(modid = "IC2")
  def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double) =
    tryChangeBuffer(directionFrom, amount * Settings.ratioIC2) / Settings.ratioIC2

  @Optional.Method(modid = "IC2")
  def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = "IC2")
  def demandedEnergyUnits = {
    if (Settings.get.ignorePower || isClient) 0
    else ForgeDirection.VALID_DIRECTIONS.map(side => globalBufferSize(side) - globalBuffer(side)).max / Settings.ratioIC2
  }
}
