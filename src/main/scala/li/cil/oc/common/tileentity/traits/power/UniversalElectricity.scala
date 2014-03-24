//package li.cil.oc.common.tileentity.traits.power
//
//import cpw.mods.fml.common.Optional
//import li.cil.oc.Settings
//import net.minecraftforge.common.util.ForgeDirection
//import universalelectricity.api.energy.{IEnergyInterface, IEnergyContainer}
//
//@Optional.InterfaceList(Array(
//  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyInterface", modid = "UniversalElectricity"),
//  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyContainer", modid = "UniversalElectricity")
//))
//trait UniversalElectricity extends Common /* with IEnergyInterface with IEnergyContainer */ {
//  @Optional.Method(modid = "UniversalElectricity")
//  override def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) =
//    (tryChangeBuffer(from, receive * Settings.ratioUE, doReceive) / Settings.ratioUE).toLong
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def getEnergy(from: ForgeDirection) = (globalBuffer(from) / Settings.ratioUE).toLong
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def getEnergyCapacity(from: ForgeDirection) = (globalBufferSize(from) / Settings.ratioUE).toLong
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def setEnergy(from: ForgeDirection, energy: Long) {}
//}
