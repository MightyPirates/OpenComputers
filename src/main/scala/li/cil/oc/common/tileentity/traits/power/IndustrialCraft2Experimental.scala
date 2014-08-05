package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

trait IndustrialCraft2Experimental extends Common with IndustrialCraft2Common {
  private var lastInjectedAmount = 0.0

  private lazy val useIndustrialCraft2Power = isServer && Mods.IndustrialCraft2.isAvailable

  // ----------------------------------------------------------------------- //

  override def validate() {
    super.validate()
    if (useIndustrialCraft2Power && !addedToIC2PowerGrid) EventHandler.scheduleIC2Add(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useIndustrialCraft2Power && addedToIC2PowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useIndustrialCraft2Power && addedToIC2PowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = Mods.IndustrialCraft2.isAvailable && canConnectPower(direction)

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double): Double = {
    lastInjectedAmount = amount
    var energy = amount * Settings.ratioIndustrialCraft2
    // Work around IC2 being uncooperative and always just passing 'unknown' along here.
    if (directionFrom == ForgeDirection.UNKNOWN) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if energy > 0) {
        energy -= tryChangeBuffer(side, energy)
      }
      energy / Settings.ratioIndustrialCraft2
    }
    else amount - tryChangeBuffer(directionFrom, energy) / Settings.ratioIndustrialCraft2
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def demandedEnergyUnits = {
    if (!useIndustrialCraft2Power) 0
    else {
      var force = false
      val demand = ForgeDirection.VALID_DIRECTIONS.map(side => {
        val size = globalBufferSize(side)
        val value = globalBuffer(side)
        val space = size - value
        force = force || (space > size / 2)
        space
      }).max / Settings.ratioIndustrialCraft2
      if (force || lastInjectedAmount <= 0 || demand >= lastInjectedAmount) demand
      else 0
    }
  }
}
