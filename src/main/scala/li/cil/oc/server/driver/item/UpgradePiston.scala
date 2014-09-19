package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.Rotatable
import li.cil.oc.api.driver.Container
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradePiston extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("pistonUpgrade"))

  override def createEnvironment(stack: ItemStack, container: Container) = container match {
    case rotatable: Rotatable with Container => new component.UpgradePiston(rotatable)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
