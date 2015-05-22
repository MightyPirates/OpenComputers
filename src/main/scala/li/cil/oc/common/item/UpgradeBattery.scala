package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.common.item.data.NodeData
import net.minecraft.item.ItemStack

class UpgradeBattery(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier with Chargeable {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.bufferCapacitorUpgrades(tier).toInt)

  override def showDurabilityBar(stack: ItemStack) = true

  override def durability(stack: ItemStack) = {
    val data = new NodeData(stack)
    1 - data.buffer.getOrElse(0.0) / Settings.get.bufferCapacitorUpgrades(tier)
  }

  // ----------------------------------------------------------------------- //

  def canCharge(stack: ItemStack): Boolean = true

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    val data = new NodeData(stack)
    val buffer = data.buffer match {
      case Some(value) => value
      case _ => 0.0
    }
    if (amount < 0) amount // TODO support discharging
    else {
      val charge = math.min(amount, Settings.get.bufferCapacitorUpgrades(tier).toInt - buffer)
      if (!simulate) {
        data.buffer = Option(buffer + charge)
        data.save(stack)
      }
      amount - charge
    }
  }
}
