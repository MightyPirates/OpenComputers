package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.inventory.DatabaseInventory
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.component
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object DriverUpgradeDatabase extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.DatabaseUpgradeTier1),
    api.Items.get(Constants.ItemName.DatabaseUpgradeTier2),
    api.Items.get(Constants.ItemName.DatabaseUpgradeTier3))

  override def createEnvironment(stack: ItemStack, host: driver.EnvironmentHost) =
    new component.UpgradeDatabase(new DatabaseInventory {
      override def tier = DriverUpgradeDatabase.tier(stack)

      override def container = stack

      override def isUseableByPlayer(player: EntityPlayer) = false
    })

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(database: item.UpgradeDatabase) => database.tier
      case _ => Tier.One
    }

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeDatabase]
}
