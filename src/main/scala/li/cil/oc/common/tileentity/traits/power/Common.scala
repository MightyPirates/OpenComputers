package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.api.network.Connector
import li.cil.oc.common.tileentity.traits.TileEntity
import net.minecraftforge.common.ForgeDirection

trait Common extends TileEntity {
  @SideOnly(Side.CLIENT)
  protected def hasConnector(side: ForgeDirection) = false

  protected def connector(side: ForgeDirection): Option[Connector] = None

  // ----------------------------------------------------------------------- //

  def canConnectPower(side: ForgeDirection) =
    !Settings.get.ignorePower && side != null && side != ForgeDirection.UNKNOWN &&
      (if (isClient) hasConnector(side) else connector(side).isDefined)

  def tryChangeBuffer(side: ForgeDirection, amount: Double, doReceive: Boolean = true) =
    if (isClient || Settings.get.ignorePower) 0
    else connector(side) match {
      case Some(node) =>
        if (doReceive) amount - node.changeBuffer(amount)
        else math.min(amount, node.globalBufferSize - node.globalBuffer)
      case _ => 0
    }

  def globalBuffer(side: ForgeDirection) =
    if (isClient) 0
    else connector(side) match {
      case Some(node) => node.globalBuffer
      case _ => 0
    }

  def globalBufferSize(side: ForgeDirection) =
    if (isClient) 0
    else connector(side) match {
      case Some(node) => node.globalBufferSize
      case _ => 0
    }
}
