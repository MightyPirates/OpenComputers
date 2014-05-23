package li.cil.oc.common.event

import net.minecraftforge.event.ForgeSubscribe
import li.cil.oc.api.event.RobotUsedTool

object TinkersConstructToolHandler {
  @ForgeSubscribe
  def onRobotApplyDamageRate(e: RobotUsedTool.ApplyDamageRate) {
    val isTinkerTool = e.toolBeforeUse.hasTagCompound && e.toolBeforeUse.getTagCompound.hasKey("InfiTool")
    if (isTinkerTool) {
      val nbtBefore = e.toolBeforeUse.getTagCompound.getCompoundTag("InfiTool")
      val nbtAfter = e.toolAfterUse.getTagCompound.getCompoundTag("InfiTool")
      val damage = nbtBefore.getInteger("Damage") - nbtAfter.getInteger("Damage")
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage =
          if (e.robot.player.getRNG.nextDouble() > 0.5)
            damage - math.floor(actualDamage).toInt
          else
            damage - math.ceil(actualDamage).toInt
        nbtAfter.setInteger("Damage", damage - repairedDamage)
      }
    }
  }
}
