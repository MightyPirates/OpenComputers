package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import micdoodle8.mods.galacticraft.api.power.EnergySource
import net.minecraftforge.common.util.ForgeDirection

trait Galacticraft extends Common {
  private implicit def toDirection(source: EnergySource) = source match {
    case adjacent: EnergySource.EnergySourceAdjacent => adjacent.direction
    case _ => ForgeDirection.UNKNOWN
  }

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def nodeAvailable(from: EnergySource) = Mods.Galacticraft.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def receiveEnergyGC(from: EnergySource, amount: Float, simulate: Boolean) =
    if (!Mods.Galacticraft.isAvailable) 0
    else (tryChangeBuffer(from, amount * Settings.ratioGalacticraft, !simulate) / Settings.ratioGalacticraft).toFloat

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def getEnergyStoredGC(from: EnergySource) = (globalBuffer(from) / Settings.ratioGalacticraft).toFloat

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def getMaxEnergyStoredGC(from: EnergySource) = (globalBufferSize(from) / Settings.ratioGalacticraft).toFloat

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def extractEnergyGC(from: EnergySource, amount: Float, simulate: Boolean) = 0f
}
