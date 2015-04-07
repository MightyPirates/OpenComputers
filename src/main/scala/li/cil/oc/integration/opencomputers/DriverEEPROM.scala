package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverEEPROM extends Item with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.EEPROM))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.EEPROM()

  override def slot(stack: ItemStack) = Slot.EEPROM

  override def tier(stack: ItemStack) = Tier.One

  override def providedEnvironment(stack: ItemStack) = classOf[component.EEPROM]
}
