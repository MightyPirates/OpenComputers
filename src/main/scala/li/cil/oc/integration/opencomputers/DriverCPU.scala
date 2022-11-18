package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.server.component
import li.cil.oc.server.machine.luac.NativeLuaArchitecture
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

object DriverCPU extends DriverCPU

abstract class DriverCPU extends Item with api.driver.item.MutableProcessor with api.driver.item.CallBudget {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.CPUTier1),
    api.Items.get(Constants.ItemName.CPUTier2),
    api.Items.get(Constants.ItemName.CPUTier3))

  override def createEnvironment(stack: ItemStack, host: api.network.EnvironmentHost): api.network.ManagedEnvironment = new component.CPU(tier(stack))

  override def slot(stack: ItemStack) = Slot.CPU

  override def tier(stack: ItemStack) = cpuTier(stack)

  def cpuTier(stack: ItemStack): Int =
    stack.getItem match {
      case cpu: item.CPU => cpu.cpuTier
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) = Settings.get.cpuComponentSupport(cpuTier(stack))

  override def allArchitectures = api.Machine.architectures.toList

  override def architecture(stack: ItemStack): Class[_ <: api.machine.Architecture] = {
    if (stack.hasTag) {
      val archClass = stack.getTag.getString(Settings.namespace + "archClass") match {
        case clazz if clazz == classOf[NativeLuaArchitecture].getName =>
          // Migrate old saved CPUs to new versions (since the class they refer still
          // exists, but is abstract, which would lead to issues).
          api.Machine.LuaArchitecture.getName
        case clazz => clazz
      }
      if (!archClass.isEmpty) try return Class.forName(archClass).asSubclass(classOf[api.machine.Architecture]) catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t)
          stack.getTag.remove(Settings.namespace + "archClass")
          stack.getTag.remove(Settings.namespace + "archName")
      }
    }
    api.Machine.architectures.headOption.orNull
  }

  override def setArchitecture(stack: ItemStack, architecture: Class[_ <: api.machine.Architecture]): Unit = {
    if (!worksWith(stack)) throw new IllegalArgumentException("Unsupported processor type.")
    val data = stack.getOrCreateTag
    data.putString(Settings.namespace + "archClass", architecture.getName)
    data.putString(Settings.namespace + "archName", api.Machine.getArchitectureName(architecture))
  }

  override def getCallBudget(stack: ItemStack): Double = Settings.get.callBudgets(tier(stack) max Tier.One min Tier.Three)
}
