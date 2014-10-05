package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeTankController extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("tankControllerUpgrade"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case robot: EnvironmentHost with Robot => new component.UpgradeTankController(robot)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Two
}
