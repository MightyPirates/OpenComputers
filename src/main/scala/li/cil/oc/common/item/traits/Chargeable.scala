package li.cil.oc.common.item.traits

import li.cil.oc.api
import net.minecraft.item.ItemStack

// TODO Forge power capabilities.
trait Chargeable extends api.driver.item.Chargeable {
  def maxCharge(stack: ItemStack): Double

  def getCharge(stack: ItemStack): Double

  def setCharge(stack: ItemStack, amount: Double): Unit
}
