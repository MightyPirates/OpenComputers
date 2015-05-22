package li.cil.oc.integration.ic2

import ic2.api.item.IElectricItemManager
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.common.item.HoverBoots
import li.cil.oc.common.item.data.HoverBootsData
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

object ElectricItemManager extends IElectricItemManager {
  override def getCharge(stack: ItemStack): Double = {
    if (stack == null) 0
    else stack.getItem match {
      // TODO in OC 1.6, add a getCharge method to Chargeable and use that instead.
      case hoverBoots: HoverBoots => new HoverBootsData(stack).charge
      case _ => 0
    }
  }

  override def charge(stack: ItemStack, amount: Double, tier: Int, ignoreTransferLimit: Boolean, simulate: Boolean): Double = {
    if (stack == null) 0
    else stack.getItem match {
      case chargeable: Chargeable =>
        val limitedAmount = if (ignoreTransferLimit) math.min(Int.MaxValue, amount) else math.min(amount, Settings.get.chargeRateTablet)
        limitedAmount - chargeable.charge(stack, limitedAmount * Settings.get.ratioIndustrialCraft2, simulate) / Settings.get.ratioIndustrialCraft2
      case _ => 0
    }
  }

  override def discharge(stack: ItemStack, amount: Double, tier: Int, ignoreTransferLimit: Boolean, externally: Boolean, simulate: Boolean): Double = {
    0.0 // TODO if we ever need it...
  }

  override def chargeFromArmor(stack: ItemStack, entity: EntityLivingBase): Unit = {}

  override def canUse(stack: ItemStack, amount: Double): Boolean = getCharge(stack) >= amount

  override def use(stack: ItemStack, amount: Double, entity: EntityLivingBase): Boolean = canUse(stack, amount) && {
    false // TODO if we ever need it...
  }

  override def getToolTip(stack: ItemStack): String = null
}
