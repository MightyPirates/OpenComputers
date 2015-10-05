package li.cil.oc.integration.cofh.energy

import cofh.api.energy.IEnergyContainerItem
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.RobotUsedToolEvent
import li.cil.oc.integration.util.Power
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

  def canCharge(stack: ItemStack): Boolean = stack.getItem match {
    case chargeable: IEnergyContainerItem => chargeable.getMaxEnergyStored(stack) > 0
    case _ => false
  }

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    stack.getItem match {
      case item: IEnergyContainerItem => amount - Power.fromRF(item.receiveEnergy(stack, Power.toRF(amount), simulate))
      case _ => amount
    }
  }
}
