package li.cil.oc.common.tileentity.traits.power

//import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.Connector
import li.cil.oc.Settings
import net.minecraftforge.common.util.ForgeDirection
//import universalelectricity.api.energy.{IEnergyInterface, IEnergyContainer}
import li.cil.oc.common.tileentity.traits.TileEntity

//@Optional.InterfaceList(Array(
//  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyInterface", modid = "UniversalElectricity"),
//  new Optional.Interface(iface = "universalelectricity.api.energy.IEnergyContainer", modid = "UniversalElectricity")
//))
trait UniversalElectricity extends TileEntity /* with IEnergyInterface with IEnergyContainer */ {
  @SideOnly(Side.CLIENT)
  protected def hasConnector(side: ForgeDirection) = false

  protected def connector(side: ForgeDirection): Option[Connector] = None

  // ----------------------------------------------------------------------- //

  override def canConnect(direction: ForgeDirection, source: AnyRef) =
    !Settings.get.ignorePower && direction != null && direction != ForgeDirection.UNKNOWN &&
      (if (isClient) hasConnector(direction) else connector(direction).isDefined)

  override def onReceiveEnergy(from: ForgeDirection, receive: Long, doReceive: Boolean) =
    if (isClient || Settings.get.ignorePower) 0
    else connector(from) match {
      case Some(node) =>
        val energy = receive / Settings.ratioBC
        if (doReceive) {
          val surplus = node.changeBuffer(energy)
          (receive - surplus * Settings.ratioBC).toLong
        }
        else {
          val space = node.globalBufferSize - node.globalBuffer
          math.min(receive, space * Settings.ratioBC).toLong
        }
      case _ => 0
    }

  override def onExtractEnergy(from: ForgeDirection, extract: Long, doExtract: Boolean) = 0

  override def setEnergy(from: ForgeDirection, energy: Long) {}

  override def getEnergy(from: ForgeDirection) =
    if (isClient) 0
    else connector(from) match {
      case Some(node) => (node.globalBuffer * Settings.ratioBC).toLong
      case _ => 0
    }

  override def getEnergyCapacity(from: ForgeDirection) =
    if (isClient) 0
    else connector(from) match {
      case Some(node) => (node.globalBufferSize * Settings.ratioBC).toLong
      case _ => 0
    }
}
