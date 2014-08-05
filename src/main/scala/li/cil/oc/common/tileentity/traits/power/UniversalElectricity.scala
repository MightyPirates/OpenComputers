package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

trait UniversalElectricity extends Common {
  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def canConnect(direction: ForgeDirection, source: AnyRef) = Mods.UniversalElectricity.isAvailable && canConnectPower(direction)

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) =
    if (!Mods.UniversalElectricity.isAvailable) 0
    else (tryChangeBuffer(from, receive * Settings.ratioUniversalElectricity, doReceive) / Settings.ratioUniversalElectricity).toLong

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def getEnergy(from: ForgeDirection) = (globalBuffer(from) / Settings.ratioUniversalElectricity).toLong

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def getEnergyCapacity(from: ForgeDirection) = (globalBufferSize(from) / Settings.ratioUniversalElectricity).toLong

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def setEnergy(from: ForgeDirection, energy: Long) {}
}
