package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.RobotUsedTool

object RobotCommonHandler {
  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedTool.ApplyDamageRate) {
    if (e.toolAfterUse.isItemStackDamageable) {
      val damage = e.toolAfterUse.getItemDamage - e.toolBeforeUse.getItemDamage
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage = if (e.robot.player.getRNG.nextDouble() > 0.5) damage - math.floor(actualDamage).toInt else damage - math.ceil(actualDamage).toInt
        e.toolAfterUse.setItemDamage(e.toolAfterUse.getItemDamage - repairedDamage)
      }
    }
  }
}
