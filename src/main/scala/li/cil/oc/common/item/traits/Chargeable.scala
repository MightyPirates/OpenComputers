package li.cil.oc.common.item.traits

import ic2.api.item.IElectricItemManager
import li.cil.oc.{Settings, api}
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.ic2.ElectricItemManager
import li.cil.oc.integration.opencomputers.ModOpenComputers
import net.minecraft.util.{EnumFacing, ResourceLocation}
import net.minecraftforge.fml.common.Optional
import net.minecraft.item.ItemStack
import net.minecraftforge.common.capabilities.{Capability, ICapabilityProvider}
import net.minecraftforge.energy.{CapabilityEnergy, IEnergyStorage}

// TODO Forge power capabilities.
@Injectable.InterfaceList(Array(
  new Injectable.Interface(value = "ic2.api.item.ISpecialElectricItem", modid = Mods.IDs.IndustrialCraft2)
))
trait Chargeable extends api.driver.item.Chargeable {

  def maxCharge(stack: ItemStack): Double

  def getCharge(stack: ItemStack): Double

  def setCharge(stack: ItemStack, amount: Double): Unit

  def canExtract(stack: ItemStack): Boolean = false

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def getManager(stack: ItemStack): IElectricItemManager = ElectricItemManager
}

object Chargeable {
  val KEY = new ResourceLocation(ModOpenComputers.getMod.id, "chargeable")

  def convertForgeEnergyToOpenComputers(fe: Int): Double = fe / Settings.get.ratioForgeEnergy

  def convertOpenComputersToForgeEnergy(oc: Double): Int = (oc * Settings.get.ratioForgeEnergy).toInt

  def applyCharge(amount: Double, current: Double, maximum: Double, save: Double => Unit): Double = {
    val target = current + amount
    val result = (target max 0) min maximum
    val used = result - current
    val unused = amount - used
    if (used > Double.MinPositiveValue || used < -Double.MinPositiveValue) {
      save(used)
    }
    unused
  }

  class Provider(stack: ItemStack, item: li.cil.oc.common.item.traits.Chargeable) extends ICapabilityProvider with IEnergyStorage {
    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
      capability == CapabilityEnergy.ENERGY
    }

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (hasCapability(capability, facing)) this.asInstanceOf[T]
      else null.asInstanceOf[T]
    }

    def receiveEnergy(maxReceive: Int, simulate: Boolean): Int =
      // Chargeable.charge() returns the amount UNUSED
      // IEnergyStorage wants the amount USED
      maxReceive - convertOpenComputersToForgeEnergy(item.charge(stack, convertForgeEnergyToOpenComputers(maxReceive), simulate))

    def extractEnergy(maxExtract: Int, simulate: Boolean): Int = {
      if (canExtract) {
        -receiveEnergy(-maxExtract, simulate)
      } else {
        0
      }
    }

    def getEnergyStored: Int = convertOpenComputersToForgeEnergy(item.getCharge(stack))

    def getMaxEnergyStored: Int = convertOpenComputersToForgeEnergy(item.maxCharge(stack))

    def canExtract: Boolean = item.canExtract(stack)

    def canReceive: Boolean = item.canCharge(stack)
  }
}
