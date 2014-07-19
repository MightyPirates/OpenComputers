package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.util.ForgeDirection
import universalelectricity.api.core.grid.electric.{IElectricNode, IEnergyContainer}
import universalelectricity.api.core.grid.{INode, INodeProvider}

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "universalelectricity.api.core.grid.INodeProvider", modid = "UniversalElectricity"),
  new Optional.Interface(iface = "universalelectricity.api.core.grid.electric.IEnergyContainer", modid = "UniversalElectricity")
))
trait UniversalElectricity extends Common with INodeProvider with IEnergyContainer {
  private lazy val ueNode: AnyRef = universalelectricity.api.core.grid.NodeRegistry.get(this, classOf[IElectricNode])

  private lazy val useUniversalElectricityPower = isServer && !Settings.get.ignorePower && Mods.BuildCraftPower.isAvailable

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useUniversalElectricityPower && world.getWorldTime % Settings.get.tickFrequency == 0) {
      val electric = ueNode.asInstanceOf[IElectricNode]
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val demand = (globalBufferSize(side) - globalBuffer(side)) / Settings.ratioUE
        val power = math.min(demand, electric.getEnergy(100))
        if (power > 1) {
          electric.drawPower(power)
          tryChangeBuffer(side, power * Settings.ratioUE)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def getNode[N <: INode](nodeType: Class[N], from: ForgeDirection) = {
    if (canConnectPower(from) && nodeType == classOf[IElectricNode]) ueNode.asInstanceOf[N]
    else null.asInstanceOf[N]
  }

  @Optional.Method(modid = "UniversalElectricity")
  override def setEnergy(from: ForgeDirection, energy: Double) {}

  @Optional.Method(modid = "UniversalElectricity")
  override def getEnergy(from: ForgeDirection) = globalBuffer(from) / Settings.ratioUE

  @Optional.Method(modid = "UniversalElectricity")
  override def getEnergyCapacity(from: ForgeDirection) = globalBufferSize(from) / Settings.ratioUE
}
