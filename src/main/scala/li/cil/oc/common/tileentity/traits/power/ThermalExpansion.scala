package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

trait ThermalExpansion extends Common {
  @Optional.Method(modid = Mods.IDs.ThermalExpansion)
  def canInterface(from: ForgeDirection) = Mods.ThermalExpansion.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.ThermalExpansion)
  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
    if (!Mods.ThermalExpansion.isAvailable) 0
    else (tryChangeBuffer(from, maxReceive * Settings.ratioThermalExpansion, !simulate) / Settings.ratioThermalExpansion).toInt

  @Optional.Method(modid = Mods.IDs.ThermalExpansion)
  def getEnergyStored(from: ForgeDirection) = (globalBuffer(from) / Settings.ratioThermalExpansion).toInt

  @Optional.Method(modid = Mods.IDs.ThermalExpansion)
  def getMaxEnergyStored(from: ForgeDirection) = (globalBufferSize(from) / Settings.ratioThermalExpansion).toInt

  @Optional.Method(modid = Mods.IDs.ThermalExpansion)
  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) = 0
}
