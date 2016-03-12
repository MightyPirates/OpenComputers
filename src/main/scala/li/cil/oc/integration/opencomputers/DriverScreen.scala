package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.component
import li.cil.oc.common.tileentity
import net.minecraft.item.ItemStack

object DriverScreen extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.BlockName.ScreenTier1))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case screen: tileentity.Screen if screen.tier > 0 => new component.Screen(screen)
    case _ => new component.TextBuffer(host)
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.Screen]
      else null
  }

}
