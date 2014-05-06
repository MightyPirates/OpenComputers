package li.cil.oc.common.event

import net.minecraftforge.event.ForgeSubscribe
import li.cil.oc.api.event.RobotUsedTool

object RobotCommonHandler {
  @ForgeSubscribe
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
