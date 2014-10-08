package li.cil.oc.integration.util

import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import universalelectricity.compatibility.Compatibility

object UniversalElectricity {
  def isEnergyItem(stack: ItemStack) = Compatibility.isHandler(stack.getItem, ForgeDirection.UNKNOWN)

  def getEnergyInItem(stack: ItemStack) = Compatibility.getEnergyItem(stack)

  def chargeItem(stack: ItemStack, value: Double) = Compatibility.chargeItem(stack, value, true)
}
