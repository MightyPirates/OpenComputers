package li.cil.oc.common.item.traits

import appeng.api.config.AccessRestriction
import cpw.mods.fml.common.Optional
import ic2.api.item.IElectricItemManager
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.ic2.ElectricItemManager
import li.cil.oc.integration.util.Power
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

@Injectable.InterfaceList(Array(
  new Injectable.Interface(value = "appeng.api.implementations.items.IAEItemPowerStorage", modid = Mods.IDs.AppliedEnergistics2),
  new Injectable.Interface(value = "cofh.api.energy.IEnergyContainerItem", modid = Mods.IDs.CoFHEnergy),
  new Injectable.Interface(value = "ic2.api.item.ISpecialElectricItem", modid = Mods.IDs.IndustrialCraft2),
  new Injectable.Interface(value = "mekanism.api.energy.IEnergizedItem", modid = Mods.IDs.Mekanism)
))
trait Chargeable extends api.driver.item.Chargeable {
  def maxCharge(stack: ItemStack): Double

  def getCharge(stack: ItemStack): Double

  def setCharge(stack: ItemStack, amount: Double): Unit

  // Applied Energistics 2

  def getAECurrentPower(stack: ItemStack): Double =
    Power.toAE(getCharge(stack))

  def getAEMaxPower(stack: ItemStack): Double =
    Power.toAE(maxCharge(stack))

  def injectAEPower(stack: ItemStack, value: Double): Double =
    Power.toAE(charge(stack, Power.fromAE(value), false))

  def extractAEPower(stack: ItemStack, value: Double): Double =
    value - Power.toAE(charge(stack, Power.fromAE(-value), false))

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def getPowerFlow(stack: ItemStack): AccessRestriction = AccessRestriction.WRITE

  // IndustrialCraft 2

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def getManager(stack: ItemStack): IElectricItemManager = ElectricItemManager

  def getMaxCharge(stack: ItemStack): Double =
    Power.toEU(maxCharge(stack))

  def getTransferLimit(stack: ItemStack): Double =
    Power.toEU(Settings.get.chargeRateTablet)

  def getTier(stack: ItemStack): Int = 1

  def canProvideEnergy(stack: ItemStack): Boolean = false

  def getEmptyItem(stack: ItemStack): Item = stack.getItem

  def getChargedItem(stack: ItemStack): Item = stack.getItem

  // Mekanism

  def getEnergy(stack: ItemStack): Double =
    Power.toJoules(getCharge(stack))

  def setEnergy(stack: ItemStack, amount: Double): Unit =
    setCharge(stack, Power.fromJoules(amount))

  def getMaxEnergy(stack: ItemStack): Double =
    Power.toJoules(maxCharge(stack))

  def canSend(stack: ItemStack): Boolean = false

  def canReceive(stack: ItemStack): Boolean = true

  def isMetadataSpecific(stack: ItemStack): Boolean = false

  def getMaxTransfer(stack: ItemStack): Double =
    Power.toJoules(Settings.get.chargeRateTablet)

  // Redstone Flux

  def getEnergyStored(stack: ItemStack): Int =
    Power.toRF(getCharge(stack))

  def getMaxEnergyStored(stack: ItemStack): Int =
    Power.toRF(maxCharge(stack))

  def receiveEnergy(stack: ItemStack, maxReceive: Int, simulate: Boolean): Int =
    maxReceive - Power.toRF(charge(stack, Power.fromRF(maxReceive), simulate))

  def extractEnergy(stack: ItemStack, maxExtract: Int, simulate: Boolean): Int =
    maxExtract - Power.toRF(charge(stack, Power.fromRF(-maxExtract), simulate))
}
