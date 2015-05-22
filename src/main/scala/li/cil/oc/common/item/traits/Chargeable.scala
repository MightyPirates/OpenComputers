package li.cil.oc.common.item.traits

import appeng.api.config.AccessRestriction
import cpw.mods.fml.common.Optional
import ic2.api.item.IElectricItemManager
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.ic2.ElectricItemManager
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
    getCharge(stack) / Settings.get.ratioAppliedEnergistics2

  def getAEMaxPower(stack: ItemStack): Double =
    maxCharge(stack) / Settings.get.ratioAppliedEnergistics2

  def injectAEPower(stack: ItemStack, value: Double): Double =
    (charge(stack, value * Settings.get.ratioAppliedEnergistics2, false) / Settings.get.ratioAppliedEnergistics2).toInt

  def extractAEPower(stack: ItemStack, value: Double): Double =
    value - (charge(stack, -value * Settings.get.ratioAppliedEnergistics2, false) / Settings.get.ratioAppliedEnergistics2).toInt

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def getPowerFlow(stack: ItemStack): AccessRestriction = AccessRestriction.WRITE

  // IndustrialCraft 2

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def getManager(stack: ItemStack): IElectricItemManager = ElectricItemManager

  def getMaxCharge(stack: ItemStack): Double =
    maxCharge(stack) / Settings.get.ratioIndustrialCraft2

  def getTransferLimit(stack: ItemStack): Double =
    Settings.get.chargeRateTablet / Settings.get.ratioIndustrialCraft2

  def getTier(stack: ItemStack): Int = 1

  def canProvideEnergy(stack: ItemStack): Boolean = false

  def getEmptyItem(stack: ItemStack): Item = stack.getItem

  def getChargedItem(stack: ItemStack): Item = stack.getItem

  // Mekanism

  def getEnergy(stack: ItemStack): Double =
    getCharge(stack) / Settings.get.ratioMekanism

  def setEnergy(stack: ItemStack, amount: Double): Unit =
    setCharge(stack, amount * Settings.get.ratioMekanism)

  def getMaxEnergy(stack: ItemStack): Double =
    maxCharge(stack) / Settings.get.ratioMekanism

  def canSend(stack: ItemStack): Boolean = false

  def canReceive(stack: ItemStack): Boolean = true

  def isMetadataSpecific(stack: ItemStack): Boolean = false

  def getMaxTransfer(stack: ItemStack): Double =
    Settings.get.chargeRateTablet / Settings.get.ratioMekanism

  // Redstone Flux

  def getEnergyStored(stack: ItemStack): Int =
    (getCharge(stack) / Settings.get.ratioRedstoneFlux).toInt

  def getMaxEnergyStored(stack: ItemStack): Int =
    (maxCharge(stack) / Settings.get.ratioRedstoneFlux).toInt

  def receiveEnergy(stack: ItemStack, maxReceive: Int, simulate: Boolean): Int =
    maxReceive - (charge(stack, maxReceive * Settings.get.ratioRedstoneFlux, simulate) / Settings.get.ratioRedstoneFlux).toInt

  def extractEnergy(stack: ItemStack, maxExtract: Int, simulate: Boolean): Int =
    maxExtract - (charge(stack, -maxExtract * Settings.get.ratioRedstoneFlux, simulate) / Settings.get.ratioRedstoneFlux).toInt
}
