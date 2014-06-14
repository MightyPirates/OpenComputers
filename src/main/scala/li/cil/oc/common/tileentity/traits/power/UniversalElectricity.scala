//package li.cil.oc.common.tileentity.traits.power
//
//import cpw.mods.fml.common.Optional
//import li.cil.oc.Settings
//import net.minecraftforge.common.util.ForgeDirection
//import universalelectricity.api.core.grid.{INode, INodeProvider}
//import universalelectricity.api.core.grid.electric.{IElectricNode, IEnergyContainer}
//
//@Optional.InterfaceList(Array(
//  new Optional.Interface(iface = "universalelectricity.api.core.grid.INodeProvider", modid = "UniversalElectricity"),
//  new Optional.Interface(iface = "universalelectricity.api.core.grid.electric.IEnergyContainer", modid = "UniversalElectricity")
//))
//trait UniversalElectricity extends Common with INodeProvider with IEnergyContainer {
//  @Optional.Method(modid = "UniversalElectricity")
//  override def canConnect(direction: ForgeDirection, source: AnyRef) = canConnectPower(direction)
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) =
//    (tryChangeBuffer(from, receive * Settings.ratioUE, doReceive) / Settings.ratioUE).toLong
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0
//
//  override def getNode[N <: INode](nodeType: Class[N], from: ForgeDirection) = {
//    if (canConnectPower(from) && nodeType == classOf[IElectricNode]) {
//
//    }
//    else null
//  }
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def setEnergy(from: ForgeDirection, energy: Double) {}
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def getEnergy(from: ForgeDirection) = globalBuffer(from) / Settings.ratioUE
//
//  @Optional.Method(modid = "UniversalElectricity")
//  override def getEnergyCapacity(from: ForgeDirection) = globalBufferSize(from) / Settings.ratioUE
//}
