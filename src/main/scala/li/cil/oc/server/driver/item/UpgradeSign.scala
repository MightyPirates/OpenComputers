package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.Rotatable
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeSign extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("signUpgrade"))

  override def createEnvironment(stack: ItemStack, container: Container) =
    container match {
      case rotatable: Container with Rotatable => new component.UpgradeSign(rotatable)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
