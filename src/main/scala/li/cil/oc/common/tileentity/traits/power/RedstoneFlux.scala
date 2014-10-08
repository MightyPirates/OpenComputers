package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.util.ForgeDirection

trait RedstoneFlux extends Common {
  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def canConnectEnergy(from: ForgeDirection) = Mods.CoFHEnergy.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
    if (!Mods.CoFHEnergy.isAvailable) 0
    else (tryChangeBuffer(from, maxReceive * Settings.get.ratioRedstoneFlux, !simulate) / Settings.get.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getEnergyStored(from: ForgeDirection) = (globalBuffer(from) / Settings.get.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getMaxEnergyStored(from: ForgeDirection) = (globalBufferSize(from) / Settings.get.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) = 0
}
