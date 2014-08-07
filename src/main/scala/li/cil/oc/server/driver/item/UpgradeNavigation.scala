package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.Rotatable
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeNavigation extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("navigationUpgrade"))

  override def createEnvironment(stack: ItemStack, container: Container) =
    container match {
      case rotatable: Container with Rotatable => new component.UpgradeNavigation(rotatable)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three
}
