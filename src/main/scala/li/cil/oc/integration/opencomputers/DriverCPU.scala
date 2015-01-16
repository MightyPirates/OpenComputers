package li.cil.oc.integration.opencomputers

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.machine.Architecture
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.init.Items
import li.cil.oc.common.item
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

object DriverCPU extends Item with Processor {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("cpu1"), api.Items.get("cpu2"), api.Items.get("cpu3"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.CPU

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => cpu.tier
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => Settings.get.cpuComponentSupport(cpu.tier)
      case _ => Tier.One
    }

  override def architecture(stack: ItemStack): Class[_ <: Architecture] = {
    if (stack.hasTagCompound) {
      val archClass = stack.getTagCompound.getString(Settings.namespace + "archClass")
      try return Class.forName(archClass).asSubclass(classOf[Architecture]) catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t)
          stack.getTagCompound.removeTag(Settings.namespace + "archClass")
          stack.getTagCompound.removeTag(Settings.namespace + "archName")
      }
    }
    api.Machine.architectures.headOption.orNull
  }
}
