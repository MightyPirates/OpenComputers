package li.cil.oc.common.event

import li.cil.oc.api.event.RobotUsedTool
import net.minecraft.item.ItemStack
import net.minecraftforge.event.ForgeSubscribe

object TinkersConstructToolHandler {
  def isTinkerTool(stack: ItemStack) = stack.hasTagCompound && stack.getTagCompound.hasKey("InfiTool")

  @ForgeSubscribe
  def onRobotApplyDamageRate(e: RobotUsedTool.ApplyDamageRate) {
    if (isTinkerTool(e.toolBeforeUse)) {
      val nbtBefore = e.toolBeforeUse.getTagCompound.getCompoundTag("InfiTool")
      val nbtAfter = e.toolAfterUse.getTagCompound.getCompoundTag("InfiTool")
      val damage = nbtAfter.getInteger("Damage") - nbtBefore.getInteger("Damage")
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage =
          if (e.robot.player.getRNG.nextDouble() > 0.5)
            damage - math.floor(actualDamage).toInt
          else
            damage - math.ceil(actualDamage).toInt
        nbtAfter.setInteger("Damage", nbtAfter.getInteger("Damage") - repairedDamage)
      }
    }
  }
}
