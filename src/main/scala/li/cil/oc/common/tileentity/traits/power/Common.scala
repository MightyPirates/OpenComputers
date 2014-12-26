package li.cil.oc.common.tileentity.traits.power

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.network.Connector
import li.cil.oc.common.tileentity.traits.TileEntity
import net.minecraft.util.EnumFacing

trait Common extends TileEntity {
  @SideOnly(Side.CLIENT)
  protected def hasConnector(side: EnumFacing) = false

  protected def connector(side: EnumFacing): Option[Connector] = None

  // ----------------------------------------------------------------------- //

  protected def energyThroughput: Double

  protected def tryAllSides(provider: (Double, EnumFacing) => Double, ratio: Double) {
    // We make sure to only call this every `Settings.get.tickFrequency` ticks,
    // but our throughput is per tick, so multiply this up for actual budget.
    var budget = energyThroughput * Settings.get.tickFrequency
    for (side <- EnumFacing.values) {
      val demand = math.min(budget, globalDemand(side)) / ratio
      if (demand > 1) {
        val energy = provider(demand, side) * ratio
        if (energy > 0) {
          budget -= tryChangeBuffer(side, energy)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def canConnectPower(side: EnumFacing) =
    !Settings.get.ignorePower && (if (isClient) hasConnector(side) else connector(side).isDefined)

  def tryChangeBuffer(side: EnumFacing, amount: Double, doReceive: Boolean = true) =
    if (isClient || Settings.get.ignorePower) 0
    else connector(side) match {
      case Some(node) =>
        val cappedAmount = math.max(0, math.min(math.min(energyThroughput, amount), globalDemand(side)))
        if (doReceive) cappedAmount - node.changeBuffer(cappedAmount)
        else cappedAmount
      case _ => 0
    }

  def globalBuffer(side: EnumFacing) =
    if (isClient) 0
    else connector(side) match {
      case Some(node) => node.globalBuffer
      case _ => 0
    }

  def globalBufferSize(side: EnumFacing) =
    if (isClient) 0
    else connector(side) match {
      case Some(node) => node.globalBufferSize
      case _ => 0
    }

  def globalDemand(side: EnumFacing) = math.max(0, math.min(energyThroughput, globalBufferSize(side) - globalBuffer(side)))
}
