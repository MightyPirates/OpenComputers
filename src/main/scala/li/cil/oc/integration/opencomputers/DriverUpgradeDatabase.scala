package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.inventory.DatabaseInventory
import li.cil.oc.common.item
import li.cil.oc.server.component
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

object DriverUpgradeDatabase extends Item with api.driver.item.HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.DatabaseUpgradeTier1),
    api.Items.get(Constants.ItemName.DatabaseUpgradeTier2),
    api.Items.get(Constants.ItemName.DatabaseUpgradeTier3))

  override def createEnvironment(stack: ItemStack, host: api.network.EnvironmentHost) =
    if (host.world != null && host.world.isClientSide) null
    else new component.UpgradeDatabase(new DatabaseInventory {
      override def container = stack

      override def stillValid(player: PlayerEntity) = false
    })

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) =
    stack.getItem match {
      case database: item.UpgradeDatabase => database.tier
      case _ => Tier.One
    }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeDatabase]
      else null
  }

}
