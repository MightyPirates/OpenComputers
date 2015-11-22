package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverWirelessNetworkCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.WirelessNetworkCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else new component.WirelessNetworkCard(host)

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = Tier.Two

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack) =
      if (worksWith(stack))
        classOf[component.WirelessNetworkCard]
      else null
  }

}
