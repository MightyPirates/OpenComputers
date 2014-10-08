package li.cil.oc.integration.cofh.energy

import cofh.api.energy.IEnergyContainerItem
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.RobotUsedToolEvent

object EventHandlerRedstoneFlux {
  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    (e.toolBeforeUse.getItem, e.toolAfterUse.getItem) match {
      case (energyBefore: IEnergyContainerItem, energyAfter: IEnergyContainerItem) =>
        val damage = energyBefore.getEnergyStored(e.toolBeforeUse) - energyAfter.getEnergyStored(e.toolAfterUse)
        if (damage > 0) {
          val actualDamage = damage * e.getDamageRate
          val repairedDamage =
            if (e.robot.player.getRNG.nextDouble() > 0.5)
              damage - math.floor(actualDamage).toInt
            else
              damage - math.ceil(actualDamage).toInt
          energyAfter.receiveEnergy(e.toolAfterUse, repairedDamage, false)
        }
      case _ =>
    }
  }
}
