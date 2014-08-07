//package li.cil.oc.common.tileentity.traits.power
//
//import cpw.mods.fml.common.Optional
//import li.cil.oc.Settings
//import li.cil.oc.util.mods.Mods
//import net.minecraftforge.common.util.ForgeDirection
//import universalelectricity.api.core.grid.electric.{IElectricNode, IEnergyContainer}
//import universalelectricity.api.core.grid.{INode, INodeProvider}
//
//@Optional.InterfaceList(Array(
//  new Optional.Interface(iface = "universalelectricity.api.core.grid.INodeProvider", modid = Mods.IDs.UniversalElectricity),
//  new Optional.Interface(iface = "universalelectricity.api.core.grid.electric.IEnergyContainer", modid = Mods.IDs.UniversalElectricity)
//))
//trait UniversalElectricity extends Common with INodeProvider with IEnergyContainer {
//  private lazy val ueNode: AnyRef = universalelectricity.api.core.grid.NodeRegistry.get(this, classOf[IElectricNode])
//
//  private lazy val useUniversalElectricityPower = isServer && !Settings.get.ignorePower && Mods.BuildCraftPower.isAvailable
//
//  // ----------------------------------------------------------------------- //
//
//  override def updateEntity() {
//    super.updateEntity()
//    if (useUniversalElectricityPower && world.getWorldTime % Settings.get.tickFrequency == 0) {
//      val electric = ueNode.asInstanceOf[IElectricNode]
//      for (side <- ForgeDirection.VALID_DIRECTIONS) {
//        val demand = (globalBufferSize(side) - globalBuffer(side)) / Settings.ratioUniversalElectricity
//        val power = math.min(demand, electric.getEnergy)
//        if (power > 1) {
//          electric.removeEnergy(power, true)
//          tryChangeBuffer(side, power * Settings.ratioUniversalElectricity)
//        }
//      }
//    }
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
//  override def getNode[N <: INode](nodeType: Class[N], from: ForgeDirection) = {
//    if (canConnectPower(from) && nodeType == classOf[IElectricNode]) ueNode.asInstanceOf[N]
//    else null.asInstanceOf[N]
//  }
//
//  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
//  override def setEnergy(from: ForgeDirection, energy: Double) {}
//
//  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
//  override def getEnergy(from: ForgeDirection) = globalBuffer(from) / Settings.ratioUniversalElectricity
//
//  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
//  override def getEnergyCapacity(from: ForgeDirection) = globalBufferSize(from) / Settings.ratioUniversalElectricity
//}
