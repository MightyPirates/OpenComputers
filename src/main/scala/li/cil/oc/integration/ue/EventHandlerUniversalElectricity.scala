package li.cil.oc.integration.ue

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.event.RobotUsedToolEvent
import net.minecraftforge.common.util.ForgeDirection
import universalelectricity.compatibility.Compatibility

object EventHandlerUniversalElectricity {
  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    if (Compatibility.isHandler(e.toolAfterUse, ForgeDirection.UNKNOWN)) {
      val damage = Compatibility.getEnergyItem(e.toolBeforeUse) - Compatibility.getEnergyItem(e.toolAfterUse)
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage =
          if (e.robot.player.getRNG.nextDouble() > 0.5)
            damage - math.floor(actualDamage).toLong
          else
            damage - math.ceil(actualDamage).toLong
        Compatibility.chargeItem(e.toolAfterUse, repairedDamage, true)
      }
    }
  }
}
