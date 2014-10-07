package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeExperience extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("experienceUpgrade"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && isRobot(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.UpgradeExperience()

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeExperience]
}
