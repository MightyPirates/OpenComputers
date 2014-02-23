package li.cil.oc.util.mods

import net.minecraft.item.ItemStack
import universalelectricity.api.CompatibilityModule

object UniversalElectricity {
  def isEnergyItem(stack: ItemStack) = CompatibilityModule.isHandler(stack.getItem)

  def getEnergyInItem(stack: ItemStack) = CompatibilityModule.getEnergyItem(stack)

  def chargeItem(stack: ItemStack, value: Long): Unit = CompatibilityModule.chargeItem(stack, value, true)
}
