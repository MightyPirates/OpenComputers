package li.cil.oc.integration.cofh.energy

import li.cil.oc.api.event.RobotUsedToolEvent
import net.minecraft.item.ItemStack

object EventHandlerRedstoneFlux {
  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    (e.toolBeforeUse.getItem, e.toolAfterUse.getItem) match {
      case (energyBefore: IEnergyContainerItem, energyAfter: IEnergyContainerItem) =>
        val damage = energyBefore.getEnergyStored(e.toolBeforeUse) - energyAfter.getEnergyStored(e.toolAfterUse)
        if (damage > 0) {
          val actualDamage = damage * e.getDamageRate
          val repairedDamage =
            if (e.agent.player.getRNG.nextDouble() > 0.5)
              damage - math.floor(actualDamage).toInt
            else
              damage - math.ceil(actualDamage).toInt
          energyAfter.receiveEnergy(e.toolAfterUse, repairedDamage, false)
        }
      case _ =>
    }
  }

  def getDurability(stack: ItemStack): Double = {
    stack.getItem match {
      case item: IEnergyContainerItem => item.getEnergyStored(stack).toDouble / item.getMaxEnergyStored(stack).toDouble
      case _ => Double.NaN
    }
  }
}
