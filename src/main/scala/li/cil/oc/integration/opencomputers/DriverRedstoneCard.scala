package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.tileentity.capabilities.RedstoneAwareImpl
import li.cil.oc.common.tileentity.traits.{BundledRedstoneAware, RedstoneAwareImpl}
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverRedstoneCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.RedstoneCardTier1),
    api.Items.get(Constants.ItemName.RedstoneCardTier2))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.getWorld != null && host.getWorld.isRemote) null
    else {
      val isAdvanced = tier(stack) == Tier.Two
      val hasBundled = BundledRedstone.isAvailable && isAdvanced
      host match {
        case redstone: BundledRedstoneAware if hasBundled =>
          new component.Redstone.Bundled(redstone)
        case redstone: RedstoneAwareImpl =>
          new component.Redstone.Vanilla(redstone)
        case _ => null
      }
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(card: item.RedstoneCard) => card.tier
      case _ => Tier.One
    }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack)) {
        val isAdvanced = tier(stack) == Tier.Two
        val hasBundled = BundledRedstone.isAvailable && isAdvanced
        if (hasBundled) {
          classOf[component.Redstone.Bundled]
        }
        else {
          classOf[component.Redstone.Vanilla]
        }
      }
      else null
  }

}
