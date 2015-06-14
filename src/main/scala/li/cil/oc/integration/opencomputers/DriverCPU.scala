package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.machine.luac.NativeLuaArchitecture
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

object DriverCPU extends DriverCPU

abstract class DriverCPU extends Item with Processor {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.CPUTier1),
    api.Items.get(Constants.ItemName.CPUTier2),
    api.Items.get(Constants.ItemName.CPUTier3))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment = null

  override def slot(stack: ItemStack) = Slot.CPU

  override def tier(stack: ItemStack) = cpuTier(stack)

  def cpuTier(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(cpu: item.CPU) => cpu.tier
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) = Settings.get.cpuComponentSupport(cpuTier(stack))

  override def architecture(stack: ItemStack): Class[_ <: Architecture] = {
    if (stack.hasTagCompound) {
      val archClass = stack.getTagCompound.getString(Settings.namespace + "archClass") match {
        case clazz if clazz == classOf[NativeLuaArchitecture].getName =>
          // Migrate old saved CPUs to new versions (since the class they refer still
          // exists, but is abstract, which would lead to issues).
          api.Machine.LuaArchitecture.getName
        case clazz => clazz
      }
      if (!archClass.isEmpty) try return Class.forName(archClass).asSubclass(classOf[Architecture]) catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t)
          stack.getTagCompound.removeTag(Settings.namespace + "archClass")
          stack.getTagCompound.removeTag(Settings.namespace + "archName")
      }
    }
    api.Machine.architectures.headOption.orNull
  }
}
