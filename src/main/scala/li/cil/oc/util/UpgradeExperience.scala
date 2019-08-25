package li.cil.oc.util

import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object UpgradeExperience {
  final val XpTag = Settings.namespace + "xp"

  def getExperience(nbt: NBTTagCompound): Double = nbt.getDouble(XpTag) max 0

  def getExperience(stack: ItemStack): Double = if (!stack.hasTagCompound) 0 else getExperience(stack.getTagCompound)

  def setExperience(nbt: NBTTagCompound, experience: Double): Unit = nbt.setDouble(XpTag, experience)

  def xpForLevel(level: Int): Double =
    if (level == 0) 0
    else Settings.get.baseXpToLevel + Math.pow(level * Settings.get.constantXpGrowth, Settings.get.exponentialXpGrowth)

  def calculateExperienceLevel(level: Int, experience: Double): Double = {
    val xpNeeded = xpForLevel(level + 1) - xpForLevel(level)
    val xpProgress = math.max(0, experience - xpForLevel(level))
    level + xpProgress / xpNeeded
  }

  def calculateLevelFromExperience(experience: Double): Int =
    math.min((Math.pow(experience - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
}
