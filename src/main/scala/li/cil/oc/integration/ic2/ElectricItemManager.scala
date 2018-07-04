package li.cil.oc.integration.ic2

import ic2.api.item.IElectricItem
import ic2.api.item.IElectricItemManager
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.integration.util.Power
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

object ElectricItemManager extends IElectricItemManager {
  override def getCharge(stack: ItemStack): Double = {
    if (stack == null) 0
    else stack.getItem match {
      case chargeable: Chargeable =>
        Power.toEU(Int.MaxValue + chargeable.charge(stack, -Int.MaxValue, true))
      case _ => 0
    }
  }

  override def charge(stack: ItemStack, amount: Double, tier: Int, ignoreTransferLimit: Boolean, simulate: Boolean): Double = {
    if (stack == null) 0
    else stack.getItem match {
      case chargeable: Chargeable =>
        val limitedAmount = if (ignoreTransferLimit) math.min(Int.MaxValue, amount) else math.min(amount, Settings.get.chargeRateTablet)
        limitedAmount - Power.toEU(chargeable.charge(stack, Power.fromEU(limitedAmount), simulate))
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

  override def getToolTip(stack: ItemStack): String = ""

  override def getMaxCharge(stack: ItemStack): Double = Option(stack).map(_.getItem) match {
    case Some(item: IElectricItem) => item.getMaxCharge(stack)
    case _ => 0
  }

  override def getTier(stack: ItemStack): Int = Option(stack).map(_.getItem) match {
    case Some(item: IElectricItem) => item.getTier(stack)
    case _ => 0
  }
}
