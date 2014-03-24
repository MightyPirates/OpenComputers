//package li.cil.oc.common.tileentity.traits.power
//
//import cpw.mods.fml.common.Optional
//import cofh.api.energy.IEnergyHandler
//import net.minecraftforge.common.ForgeDirection
//import li.cil.oc.Settings
//
//@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "ThermalExpansion")
//trait ThermalExpansion extends Common with IEnergyHandler {
//  @Optional.Method(modid = "ThermalExpansion")
//  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
//    (onReceiveEnergy(from, (maxReceive * Settings.ratioTE).toLong, !simulate) / Settings.ratioTE).toInt
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) =
//    (onExtractEnergy(from, (maxExtract * Settings.ratioTE).toLong, !simulate) / Settings.ratioTE).toInt
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def canInterface(from: ForgeDirection) = canConnect(from, this)
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def getEnergyStored(from: ForgeDirection) = (getEnergy(from) / Settings.ratioTE).toInt
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def getMaxEnergyStored(from: ForgeDirection) = (getEnergyCapacity(from) / Settings.ratioTE).toInt
//}
