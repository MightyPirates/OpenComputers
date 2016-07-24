package li.cil.oc.integration.gregtech

import li.cil.oc.api.event.RobotUsedToolEvent
import net.minecraft.item.ItemStack

object EventHandlerGregTech {
  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    (e.toolBeforeUse.getItem, e.toolAfterUse.getItem) match {
      case (itemBefore: IDamagableItem, itemAfter: IDamagableItem) =>
        val damage = GT_MetaGenerated_Tool.getToolDamage(e.toolAfterUse) - GT_MetaGenerated_Tool.getToolDamage(e.toolBeforeUse)
        if (damage > 0) {
          val actualDamage = damage * e.getDamageRate
          val repairedDamage =
            if (e.agent.player.getRNG.nextDouble() > 0.5)
              damage - math.floor(actualDamage).toInt
            else
              damage - math.ceil(actualDamage).toInt
          GT_MetaGenerated_Tool.setToolDamage(e.toolAfterUse, GT_MetaGenerated_Tool.getToolDamage(e.toolAfterUse) - repairedDamage)
        }
      case _ =>
    }
  }

  def getDurability(stack: ItemStack): Double = {
    stack.getItem match {
      case item: IDamagableItem => 1.0 - GT_MetaGenerated_Tool.getToolDamage(stack).toDouble / GT_MetaGenerated_Tool.getToolMaxDamage(stack).toDouble
      case _ => Double.NaN
    }
  }
}
