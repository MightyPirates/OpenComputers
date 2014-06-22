package li.cil.oc.common.event

import li.cil.oc.api.event.RobotUsedTool
import li.cil.oc.util.mods.UniversalElectricity
import net.minecraftforge.event.ForgeSubscribe

object UniversalElectricityToolHandler {
  @ForgeSubscribe
  def onRobotApplyDamageRate(e: RobotUsedTool.ApplyDamageRate) {
    if (UniversalElectricity.isEnergyItem(e.toolAfterUse)) {
      val damage = UniversalElectricity.getEnergyInItem(e.toolBeforeUse) - UniversalElectricity.getEnergyInItem(e.toolAfterUse)
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage =
          if (e.robot.player.getRNG.nextDouble() > 0.5)
            damage - math.floor(actualDamage).toLong
          else
            damage - math.ceil(actualDamage).toLong
        UniversalElectricity.chargeItem(e.toolAfterUse, repairedDamage)
      }
    }
  }
}
