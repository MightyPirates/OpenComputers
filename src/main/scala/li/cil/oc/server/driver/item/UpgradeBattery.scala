package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.item
import li.cil.oc.server.component
import li.cil.oc.{Items, api}
import net.minecraft.item.ItemStack

object UpgradeBattery extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("batteryUpgrade1"), api.Items.get("batteryUpgrade2"), api.Items.get("batteryUpgrade3"))

  override def createEnvironment(stack: ItemStack, container: Container) = new component.UpgradeBattery(tier(stack))

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(battery: item.UpgradeBattery) => battery.tier
      case _ => 0
    }
}
