package li.cil.oc.server.component

import li.cil.oc.api.event.RobotPlaceInAirEvent
import li.cil.oc.api.internal
import li.cil.oc.util.BlockPosition
import net.minecraftforge.common.MinecraftForge

trait Agent extends traits.WorldControl with traits.InventoryControl with traits.InventoryWorldControl with traits.TankAware with traits.TankControl with traits.TankWorldControl {
  def agent: internal.Agent

  override def position = BlockPosition(agent)

  override def fakePlayer = agent.player

  // ----------------------------------------------------------------------- //

  override def inventory = agent.mainInventory

  override def selectedSlot = agent.selectedSlot

  override def selectedSlot_=(value: Int): Unit = agent.setSelectedSlot(value)

  // ----------------------------------------------------------------------- //

  override def tank = agent.tank

  def selectedTank = agent.selectedTank

  override def selectedTank_=(value: Int) = agent.setSelectedTank(value)

  // ----------------------------------------------------------------------- //

  def canPlaceInAir = {
    val event = new RobotPlaceInAirEvent(agent)
    MinecraftForge.EVENT_BUS.post(event)
    event.isAllowed
  }

}
