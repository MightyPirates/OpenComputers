package li.cil.oc.util.mods

import net.minecraft.item.ItemStack

// TODO Upgrade to UE 1.7 once it's available.
//import universalelectricity.api.CompatibilityModule

object UniversalElectricity {
  def isEnergyItem(stack: ItemStack) = false // CompatibilityModule.isHandler(stack.getItem)

  def getEnergyInItem(stack: ItemStack) = 0 // CompatibilityModule.getEnergyItem(stack)

  def chargeItem(stack: ItemStack, value: Long): Unit = {} // CompatibilityModule.chargeItem(stack, value, true)
}
