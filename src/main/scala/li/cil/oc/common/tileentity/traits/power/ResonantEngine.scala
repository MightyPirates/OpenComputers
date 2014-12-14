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
import resonant.api.electric.IEnergyNode
import resonant.api.grid.INode
import resonant.api.grid.INodeProvider
import resonant.lib.grid.electric.DCNode

@Injectable.Interface(value = "resonant.api.grid.INodeProvider", modid = Mods.IDs.ResonantEngine)
trait ResonantEngine extends Common {
  private var node: Option[AnyRef] = None

  private lazy val useResonantEnginePower = isServer && Mods.ResonantEngine.isAvailable

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useResonantEnginePower && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  @Optional.Method(modid = Mods.IDs.ResonantEngine)
  private def updateEnergy() {
    node match {
      case Some(energyNode: DCNode) =>
        tryAllSides((demand, _) => energyNode.extractEnergy(demand, doExtract = true), Settings.get.ratioResonantEngine)
      case _ =>
    }
  }

  override def validate() {
    super.validate()
    if (useResonantEnginePower) EventHandler.scheduleUEAdd(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useResonantEnginePower) deconstructNode()
  }

  @Optional.Method(modid = Mods.IDs.ResonantEngine)
  private def deconstructNode() {
    getNode(classOf[IEnergyNode], ForgeDirection.UNKNOWN).deconstruct()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (useResonantEnginePower) loadNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.ResonantEngine)
  private def loadNode(nbt: NBTTagCompound) {
    node match {
      case Some(energyNode: DCNode) => energyNode.getEnergyStorage().readFromNBT(nbt.getCompoundTag("repower"))
      case _ =>
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (useResonantEnginePower) saveNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.ResonantEngine)
  private def saveNode(nbt: NBTTagCompound) {
    node match {
      case Some(energyNode: DCNode) => nbt.setNewCompoundTag("repower", energyNode.getEnergyStorage().writeToNBT)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.ResonantEngine)
  def getNode[N <: INode](nodeType: Class[_ <: N], side: ForgeDirection): N = {
    if (nodeType != null && nodeType.isAssignableFrom(classOf[DCNode])) node match {
      case Some(energyNode: DCNode) => energyNode.asInstanceOf[N]
      case _ =>
        this match {
          case nodeProvider: INodeProvider =>
            val energyNode = new DCNode(nodeProvider) {
              override def canConnect(from: ForgeDirection) = canConnectPower(from) && super.canConnect(from)
            }
            node = Option(energyNode)
            energyNode.asInstanceOf[N]
          case _ =>
            OpenComputers.log.warn("Failed setting up UniversalElectricity power, which most likely means the class transformer did not run. You're probably running in an incorrectly configured development environment. Try adding `-Dfml.coreMods.load=li.cil.oc.common.launch.TransformerLoader` to the VM options of your run configuration.")
            null.asInstanceOf[N]
        }
    }
    else null.asInstanceOf[N]
  }
}
