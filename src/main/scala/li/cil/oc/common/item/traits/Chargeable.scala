package li.cil.oc.common.item.traits

import ic2.api.item.IElectricItemManager
import li.cil.oc.api
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.ic2.ElectricItemManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.Optional

// TODO Forge power capabilities.
@Injectable.InterfaceList(Array(
  new Injectable.Interface(value = "ic2.api.item.ISpecialElectricItem", modid = Mods.IDs.IndustrialCraft2)
))
trait Chargeable extends api.driver.item.Chargeable {
  def maxCharge(stack: ItemStack): Double

  def getCharge(stack: ItemStack): Double

  def setCharge(stack: ItemStack, amount: Double): Unit

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def getManager(stack: ItemStack): IElectricItemManager = ElectricItemManager
}
