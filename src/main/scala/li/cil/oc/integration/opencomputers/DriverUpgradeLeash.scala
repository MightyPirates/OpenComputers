package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack

object DriverUpgradeLeash extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.LeashUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case entity: Entity => new component.UpgradeLeash(entity)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.One

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeLeash]
}
