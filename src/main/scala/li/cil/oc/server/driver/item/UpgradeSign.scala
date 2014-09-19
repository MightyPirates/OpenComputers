package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeSign extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("signUpgrade"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    host match {
      case rotatable: EnvironmentHost with Rotatable => new component.UpgradeSign(rotatable)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
