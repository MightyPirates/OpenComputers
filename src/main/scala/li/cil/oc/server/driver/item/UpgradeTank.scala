package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{Container, Inventory, Slot}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeTank extends Item  {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("tankUpgrade"))

  override def createEnvironment(stack: ItemStack, container: Container) = new  component.UpgradeTank(container,tankCapacity)

  override def slot(stack: ItemStack) = Slot.Upgrade

   def tankCapacity() = 16000
}
