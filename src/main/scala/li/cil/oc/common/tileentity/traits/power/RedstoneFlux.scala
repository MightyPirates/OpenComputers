package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.util.ForgeDirection

trait RedstoneFlux extends Common {
  @Optional.Method(modid = Mods.IDs.RedstoneFlux)
  def canConnectEnergy(from: ForgeDirection) = Mods.RedstoneFlux.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.RedstoneFlux)
  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
    if (!Mods.RedstoneFlux.isAvailable) 0
    else (tryChangeBuffer(from, maxReceive * Settings.ratioRedstoneFlux, !simulate) / Settings.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.RedstoneFlux)
  def getEnergyStored(from: ForgeDirection) = (globalBuffer(from) / Settings.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.RedstoneFlux)
  def getMaxEnergyStored(from: ForgeDirection) = (globalBufferSize(from) / Settings.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.RedstoneFlux)
  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) = 0
}
