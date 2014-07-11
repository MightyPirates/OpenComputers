package li.cil.oc.server.component

import li.cil.oc.api.network.{Arguments, Callback, Context, Visibility}
import li.cil.oc.common.component
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound

class UpgradeExperience extends component.ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("experience").
    withConnector(30 * Settings.get.bufferPerLevel).
    create()

  var experience = 0.0

  var level = 0

  def xpForLevel(level: Int) =
    if (level == 0) 0
    else Settings.get.baseXpToLevel + Math.pow(level * Settings.get.constantXpGrowth, Settings.get.exponentialXpGrowth)

  def xpForNextLevel = xpForLevel(level + 1)

  def addExperience(value: Double) {
    if (level < 30) {
      experience = experience + value
      if (experience >= xpForNextLevel) {
        updateXpInfo()
      }
    }
  }

  def updateXpInfo() {
    // xp(level) = base + (level * const) ^ exp
    // pow(xp(level) - base, 1/exp) / const = level
    level = math.min((Math.pow(experience - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
    if (node != null) {
      node.setLocalBufferSize(Settings.get.bufferPerLevel * level)
    }
  }

  @Callback(direct = true, doc = """function():number -- The current level of experience stored in this experience upgrade.""")
  def level(context: Context, args: Arguments): Array[AnyRef] = {
    val xpNeeded = xpForNextLevel - xpForLevel(level)
    val xpProgress = math.max(0, experience - xpForLevel(level))
    result(level + xpProgress / xpNeeded)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble(Settings.namespace + "xp", experience)
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    experience = nbt.getDouble(Settings.namespace + "xp") max 0
    updateXpInfo()
  }
}
