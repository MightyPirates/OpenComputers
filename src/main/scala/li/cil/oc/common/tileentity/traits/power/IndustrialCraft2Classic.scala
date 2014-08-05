package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import ic2classic.api.Direction
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.util.mods.Mods
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

trait IndustrialCraft2Classic extends Common with IndustrialCraft2Common {
  private var lastInjectedAmount = 0.0

  private lazy val useIndustrialCraft2ClassicPower = isServer && Mods.IndustrialCraft2Classic.isAvailable

  // ----------------------------------------------------------------------- //

  override def validate() {
    super.validate()
    if (useIndustrialCraft2ClassicPower && !addedToIC2PowerGrid) EventHandler.scheduleIC2Add(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useIndustrialCraft2ClassicPower && addedToIC2PowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useIndustrialCraft2ClassicPower && addedToIC2PowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def isAddedToEnergyNet = addedToIC2PowerGrid

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def acceptsEnergyFrom(emitter: TileEntity, direction: Direction) = Mods.IndustrialCraft2Classic.isAvailable && canConnectPower(direction.toForgeDirection)

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def injectEnergy(directionFrom: Direction, amount: Int) = {
    lastInjectedAmount = amount
    var energy = amount * Settings.ratioIndustrialCraft2
    // Work around IC2 being uncooperative and always just passing 'unknown' along here.
    if (directionFrom.toForgeDirection == ForgeDirection.UNKNOWN) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if energy > 0) {
        energy -= tryChangeBuffer(side, energy)
      }
      (energy / Settings.ratioIndustrialCraft2).toInt == 0
    }
    else (amount - tryChangeBuffer(directionFrom.toForgeDirection, energy) / Settings.ratioIndustrialCraft2).toInt == 0
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def demandsEnergy = {
    if (!useIndustrialCraft2ClassicPower) 0
    else {
      var force = false
      val demand = ForgeDirection.VALID_DIRECTIONS.map(side => {
        val size = globalBufferSize(side)
        val value = globalBuffer(side)
        val space = size - value
        force = force || (space > size / 2)
        space
      }).max / Settings.ratioIndustrialCraft2
      if (force || lastInjectedAmount <= 0 || demand >= lastInjectedAmount) demand.toInt
      else 0
    }
  }
}
