package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection
import universalelectricity.api.energy.{IEnergyContainer, IEnergyInterface}

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyInterface", modid = Mods.IDs.UniversalElectricity),
  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyContainer", modid = Mods.IDs.UniversalElectricity)
))
trait UniversalElectricity extends Common with IEnergyInterface with IEnergyContainer {
  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  override def canConnect(direction: ForgeDirection, source: AnyRef) = canConnectPower(direction)

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  override def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) =
    (tryChangeBuffer(from, receive * Settings.ratioUE, doReceive) / Settings.ratioUE).toLong

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  override def getEnergy(from: ForgeDirection) = (globalBuffer(from) / Settings.ratioUE).toLong

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  override def getEnergyCapacity(from: ForgeDirection) = (globalBufferSize(from) / Settings.ratioUE).toLong

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  override def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  override def setEnergy(from: ForgeDirection, energy: Long) {}
}
