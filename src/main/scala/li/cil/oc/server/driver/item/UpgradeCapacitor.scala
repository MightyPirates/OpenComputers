package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.server.component.Container
import net.minecraft.item.ItemStack

object UpgradeCapacitor extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("capacitorUpgrade"))

  override def createEnvironment(stack: ItemStack, container: Container) = new component.UpgradeCapacitor()

  override def slot(stack: ItemStack) = Slot.Upgrade
}
