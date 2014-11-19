package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.integration.Mods
import micdoodle8.mods.galacticraft.api.power.EnergySource
import net.minecraftforge.common.util.ForgeDirection

import scala.language.implicitConversions

trait Galacticraft extends Common {
  private implicit def toDirection(source: EnergySource): ForgeDirection = source match {
    case adjacent: EnergySource.EnergySourceAdjacent => adjacent.direction
    case _ => ForgeDirection.UNKNOWN
  }

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def nodeAvailable(from: EnergySource) = Mods.Galacticraft.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def receiveEnergyGC(from: EnergySource, amount: Float, simulate: Boolean) =
    if (!Mods.Galacticraft.isAvailable) 0
    else (tryChangeBuffer(from, amount * Settings.get.ratioGalacticraft, !simulate) / Settings.get.ratioGalacticraft).toFloat

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def getEnergyStoredGC(from: EnergySource) = (globalBuffer(from) / Settings.get.ratioGalacticraft).toFloat

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def getMaxEnergyStoredGC(from: EnergySource) = (globalBufferSize(from) / Settings.get.ratioGalacticraft).toFloat

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def extractEnergyGC(from: EnergySource, amount: Float, simulate: Boolean) = 0f
}
