package li.cil.oc.server.driver.item

import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.{Slot, Tier, item}
import li.cil.oc.{Items, api}
import net.minecraft.item.ItemStack

object Memory extends Item with driver.Memory {
  override def amount(stack: ItemStack) = Items.multi.subItem(stack) match {
    case Some(memory: item.Memory) => memory.kiloBytes * 1024
    case _ => 0
  }

  override def worksWith(stack: ItemStack, host: EnvironmentHost) = isOneOf(stack, api.Items.get("ram1"), api.Items.get("ram2"), api.Items.get("ram3"), api.Items.get("ram4"), api.Items.get("ram5"), api.Items.get("ram6"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Memory

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(memory: item.Memory) => memory.tier / 2
      case _ => Tier.One
    }
}
