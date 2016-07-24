package li.cil.oc.integration.tcon

import li.cil.oc.api.event.RobotUsedToolEvent
import net.minecraft.item.ItemStack

object EventHandlerTinkersConstruct {
  def isTinkerTool(stack: ItemStack) = stack.hasTagCompound && stack.getTagCompound.hasKey("InfiTool")

  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    if (isTinkerTool(e.toolBeforeUse)) {
      val nbtBefore = e.toolBeforeUse.getTagCompound.getCompoundTag("InfiTool")
      val nbtAfter = e.toolAfterUse.getTagCompound.getCompoundTag("InfiTool")
      val damage = nbtAfter.getInteger("Damage") - nbtBefore.getInteger("Damage")
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage =
          if (e.agent.player.getRNG.nextDouble() > 0.5)
            damage - math.floor(actualDamage).toInt
          else
            damage - math.ceil(actualDamage).toInt
        nbtAfter.setInteger("Damage", nbtAfter.getInteger("Damage") - repairedDamage)
      }
    }
  }

  def getDurability(stack: ItemStack): Double = {
    if (isTinkerTool(stack)) {
      val nbt = stack.getTagCompound.getCompoundTag("InfiTool")
      if (nbt.getBoolean("Broken")) 0.0
      else {
        val damage = nbt.getInteger("Damage")
        val maxDamage = nbt.getInteger("TotalDurability")
        1.0 - damage.toDouble / maxDamage.toDouble
      }
    }
    else Double.NaN
  }
}
