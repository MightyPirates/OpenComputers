package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.network.Connector
import li.cil.oc.common.tileentity.traits.TileEntity
import net.minecraftforge.common.util.ForgeDirection

trait Common extends TileEntity {
  @SideOnly(Side.CLIENT)
  protected def hasConnector(side: ForgeDirection) = false

  protected def connector(side: ForgeDirection): Option[Connector] = None

  // ----------------------------------------------------------------------- //

  def energyThroughput: Double

  protected def tryAllSides(provider: (Double, ForgeDirection) => Double, fromOther: Double => Double, toOther: Double => Double) {
    // We make sure to only call this every `Settings.get.tickFrequency` ticks,
    // but our throughput is per tick, so multiply this up for actual budget.
    var budget = energyThroughput * Settings.get.tickFrequency
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      val demand = toOther(math.min(budget, globalDemand(side)))
      if (demand > 1) {
        val energy = fromOther(provider(demand, side))
        if (energy > 0) {
          budget -= tryChangeBuffer(side, energy)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def canConnectPower(side: ForgeDirection) =
    !Settings.get.ignorePower && side != null && side != ForgeDirection.UNKNOWN &&
      (if (isClient) hasConnector(side) else connector(side).isDefined)

  /**
   * Tries to inject the specified amount of energy into the buffer via the specified side.
   *
   * @param side the side to change the buffer through.
   * @param amount the amount to change the buffer by.
   * @param doReceive whether to actually inject energy or only simulate it.
   * @return the amount of energy that was actually injected.
   */
  def tryChangeBuffer(side: ForgeDirection, amount: Double, doReceive: Boolean = true): Double =
    if (isClient || Settings.get.ignorePower) 0
    else connector(side) match {
      case Some(node) =>
        val cappedAmount = math.max(0, math.min(math.min(energyThroughput, amount), globalDemand(side)))
        if (doReceive) cappedAmount - node.changeBuffer(cappedAmount)
        else cappedAmount
      case _ => 0
    }

  def globalBuffer(side: ForgeDirection): Double =
    if (isClient) 0
    else connector(side) match {
      case Some(node) => node.globalBuffer
      case _ => 0
    }

  def globalBufferSize(side: ForgeDirection): Double =
    if (isClient) 0
    else connector(side) match {
      case Some(node) => node.globalBufferSize
      case _ => 0
    }

  def globalDemand(side: ForgeDirection) = math.max(0, math.min(energyThroughput, globalBufferSize(side) - globalBuffer(side)))
}
