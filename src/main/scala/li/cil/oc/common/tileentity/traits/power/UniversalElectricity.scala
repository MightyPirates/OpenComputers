package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import universalelectricity.api.core.grid.INode
import universalelectricity.api.core.grid.INodeProvider
import universalelectricity.api.core.grid.electric.IEnergyNode
import universalelectricity.core.grid.node.NodeEnergy

@Injectable.Interface(value = "universalelectricity.api.core.grid.INodeProvider", modid = Mods.IDs.UniversalElectricity)
trait UniversalElectricity extends Common {
  private var node: Option[AnyRef] = None

  private lazy val useUniversalElectricityPower = isServer && Mods.UniversalElectricity.isAvailable

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useUniversalElectricityPower && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  private def updateEnergy() {
    node match {
      case Some(energyNode: NodeEnergy) =>
        tryAllSides((demand, _) => energyNode.buffer.extractEnergy(demand, doExtract = true), Settings.get.ratioUniversalElectricity)
      case _ =>
    }
  }

  override def validate() {
    super.validate()
    if (useUniversalElectricityPower) EventHandler.scheduleUEAdd(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useUniversalElectricityPower) deconstructNode()
  }

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  private def deconstructNode() {
    getNode(classOf[IEnergyNode], ForgeDirection.UNKNOWN).deconstruct()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (useUniversalElectricityPower) loadNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  private def loadNode(nbt: NBTTagCompound) {
    node match {
      case Some(energyNode: NodeEnergy) => energyNode.load(nbt.getCompoundTag("uepower"))
      case _ =>
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (useUniversalElectricityPower) saveNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  private def saveNode(nbt: NBTTagCompound) {
    node match {
      case Some(energyNode: NodeEnergy) => nbt.setNewCompoundTag("uepower", energyNode.save)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.UniversalElectricity)
  def getNode(nodeType: Class[_ <: INode], side: ForgeDirection): INode = {
    if (nodeType != null && classOf[IEnergyNode].isAssignableFrom(nodeType)) node match {
      case Some(energyNode: NodeEnergy) => energyNode
      case _ =>
        this match {
          case nodeProvider: INodeProvider =>
            val conversionBufferSize = energyThroughput * Settings.get.tickFrequency / Settings.get.ratioUniversalElectricity
            val energyNode = new NodeEnergy(nodeProvider, conversionBufferSize, conversionBufferSize, conversionBufferSize) {
              override def canConnect(from: ForgeDirection) = canConnectPower(from) && super.canConnect(from)
            }
            node = Option(energyNode)
            energyNode
          case _ =>
            OpenComputers.log.warn("Failed setting up UniversalElectricity power, which most likely means the class transformer did not run. You're probably running in an incorrectly configured development environment. Try adding `-Dfml.coreMods.load=li.cil.oc.common.launch.TransformerLoader` to the VM options of your run configuration.")
            null
        }
    }
    else null
  }
}
