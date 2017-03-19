package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.component
import li.cil.oc.server.machine.luac.NativeLuaArchitecture
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object DriverCPU extends DriverCPU

abstract class DriverCPU extends Item with api.driver.item.MutableProcessor with api.driver.item.CallBudget {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.CPUTier1),
    api.Items.get(Constants.ItemName.CPUTier2),
    api.Items.get(Constants.ItemName.CPUTier3))

  override def createEnvironment(stack: ItemStack, host: Location): NodeContainerItem = new component.CPU(tier(stack))

  override def slot(stack: ItemStack) = Slot.CPU

  override def tier(stack: ItemStack) = cpuTier(stack)

  def cpuTier(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(cpu: item.CPU) => cpu.cpuTier
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) = Settings.Computer.cpuComponentCount(cpuTier(stack))

  override def allArchitectures = api.Machine.architectures.toList

  override def architecture(stack: ItemStack): Class[_ <: api.machine.Architecture] = {
    if (stack.hasTagCompound) {
      val archClass = stack.getTagCompound.getString(Constants.namespace + "archClass") match {
        case clazz if clazz == classOf[NativeLuaArchitecture].getName =>
          // Migrate old saved CPUs to new versions (since the class they refer still
          // exists, but is abstract, which would lead to issues).
          api.Machine.LuaArchitecture.getName
        case clazz => clazz
      }
      if (!archClass.isEmpty) try return Class.forName(archClass).asSubclass(classOf[api.machine.Architecture]) catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t)
          stack.getTagCompound.removeTag(Constants.namespace + "archClass")
          stack.getTagCompound.removeTag(Constants.namespace + "archName")
      }
    }
    api.Machine.architectures.headOption.orNull
  }

  override def setArchitecture(stack: ItemStack, architecture: Class[_ <: api.machine.Architecture]): Unit = {
    if (!worksWith(stack)) throw new IllegalArgumentException("Unsupported processor type.")
    if (!stack.hasTagCompound) stack.setTagCompound(new NBTTagCompound())
    stack.getTagCompound.setString(Constants.namespace + "archClass", architecture.getName)
    stack.getTagCompound.setString(Constants.namespace + "archName", api.Machine.getArchitectureName(architecture))
  }

  override def getCallBudget(stack: ItemStack): Double = Settings.Computer.callBudgets(tier(stack) max Tier.One min Tier.Three)
}
