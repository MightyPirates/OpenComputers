package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.util.ForgeDirection

@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = Mods.IDs.IndustrialCraft2)
trait IndustrialCraft2 extends Common with IEnergySink {
  var addedToPowerGrid = false

  private var lastInjectedAmount = 0.0

  private lazy val useIndustrialCraft2Power = isServer && !Settings.get.ignorePower && Mods.IndustrialCraft2.isAvailable

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

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = canConnectPower(direction)

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  override def injectEnergy(directionFrom: ForgeDirection, amount: Double, voltage: Double): Double = {
    lastInjectedAmount = amount
    var energy = amount * Settings.ratioIC2
    // Work around IC2 being uncooperative and always just passing 'unknown' along here.
    if (directionFrom == ForgeDirection.UNKNOWN) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if energy > 0) {
        energy -= tryChangeBuffer(side, energy)
      }
      energy / Settings.ratioIC2
    }
    else amount - tryChangeBuffer(directionFrom, energy) / Settings.ratioIC2
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  override def getSinkTier = Int.MaxValue

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  override def getDemandedEnergy = {
    if (Settings.get.ignorePower || isClient) 0
    else {
      var force = false
      val demand = ForgeDirection.VALID_DIRECTIONS.map(side => {
        val size = globalBufferSize(side)
        val value = globalBuffer(side)
        val space = size - value
        force = force || (space > size / 2)
        space
      }).max / Settings.ratioIC2
      if (force || lastInjectedAmount <= 0 || demand >= lastInjectedAmount) demand
      else 0
    }
  }
}
