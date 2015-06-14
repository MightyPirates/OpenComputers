package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Items
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsScala._

class UpgradeExperience(val host: EnvironmentHost with internal.Agent) extends prefab.ManagedEnvironment {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("experience").
    withConnector(30 * Settings.get.bufferPerLevel).
    create()

  var experience = 0.0

  var level = 0

  def xpForLevel(level: Int): Double =
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

  @Callback(doc = """function():boolean -- Tries to consume an enchanted item to add experience to the upgrade.""")
  def consume(context: Context, args: Arguments): Array[AnyRef] = {
    val stack = host.mainInventory.getStackInSlot(host.selectedSlot)
    if (stack == null || stack.stackSize < 1) {
      return result(Unit, "no item")
    }
    var xp = 0
    if (stack.getItem == Items.experience_bottle) {
      xp += 3 + host.world.rand.nextInt(5) + host.world.rand.nextInt(5)
    }
    else {
      for ((id: Int, level: Int) <- EnchantmentHelper.getEnchantments(stack)) {
        val enchantment = Enchantment.enchantmentsList(id)
        if (enchantment != null) {
          xp += enchantment.getMinEnchantability(level)
        }
      }
      if (xp <= 0) {
        return result(Unit, "could not extract experience from item")
      }
    }
    val consumed = host.mainInventory().decrStackSize(host.selectedSlot, 1)
    if (consumed == null || consumed.stackSize < 1) {
      return result(Unit, "could not consume item")
    }
    addExperience(xp * Settings.get.constantXpGrowth)
    result(true)
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
