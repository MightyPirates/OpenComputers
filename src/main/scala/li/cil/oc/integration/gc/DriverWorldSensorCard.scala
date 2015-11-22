package li.cil.oc.integration.gc

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.integration.Mods
import li.cil.oc.integration.opencomputers.Item
import net.minecraft.item.ItemStack

object DriverWorldSensorCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get(Constants.ItemName.WorldSensorCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (Mods.Galacticraft.isAvailable) new WorldSensorCard(host)
    else null

  override def slot(stack: ItemStack) = Slot.Card

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[WorldSensorCard]
      else null
  }

}
