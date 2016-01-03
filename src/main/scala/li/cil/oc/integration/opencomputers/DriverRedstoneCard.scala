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
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.WirelessRedstone
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverRedstoneCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.RedstoneCardTier1),
    api.Items.get(Constants.ItemName.RedstoneCardTier2))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world != null && host.world.isRemote) null
    else {
      val isAdvanced = tier(stack) == Tier.Two
      val hasBundled = BundledRedstone.isAvailable && isAdvanced
      val hasWireless = WirelessRedstone.isAvailable && isAdvanced
      host match {
        case redstone: BundledRedstoneAware if hasBundled =>
          if (hasWireless) new component.Redstone.BundledWireless(redstone)
          else new component.Redstone.Bundled(redstone)
        case redstone: RedstoneAware =>
          if (hasWireless) new component.Redstone.VanillaWireless(redstone)
          else new component.Redstone.Vanilla(redstone)
        case _ =>
          if (hasWireless) new component.Redstone.Wireless(host)
          else null
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
        val hasWireless = WirelessRedstone.isAvailable && isAdvanced
        if (hasBundled) {
          if (hasWireless) classOf[component.Redstone.BundledWireless]
          else classOf[component.Redstone.Bundled]
        }
        else {
          classOf[component.Redstone.Vanilla]
        }
      }
      else null
  }

}
