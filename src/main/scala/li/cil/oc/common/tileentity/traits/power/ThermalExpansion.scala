//package li.cil.oc.common.tileentity.traits.power
//
//import cofh.api.energy.IEnergyHandler
//import cpw.mods.fml.common.Optional
//import li.cil.oc.Settings
//import net.minecraftforge.common.ForgeDirection
//
//@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "ThermalExpansion")
//trait ThermalExpansion extends Common with IEnergyHandler {
//  @Optional.Method(modid = "ThermalExpansion")
//  def canInterface(from: ForgeDirection) = canConnectPower(from)
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
//    (tryChangeBuffer(from, maxReceive * Settings.ratioTE, !simulate) / Settings.ratioTE).toInt
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def getEnergyStored(from: ForgeDirection) = (globalBuffer(from) / Settings.ratioTE).toInt
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def getMaxEnergyStored(from: ForgeDirection) = (globalBufferSize(from) / Settings.ratioTE).toInt
//
//  @Optional.Method(modid = "ThermalExpansion")
//  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) = 0
//}
