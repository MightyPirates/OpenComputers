package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.Environment
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.init.Items
import li.cil.oc.common.item
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.WirelessRedstone
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverRedstoneCard extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("redstoneCard1"), api.Items.get("redstoneCard2"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && (isComputer(host) || isRobot(host) || isServer(host))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    host match {
      case redstone: BundledRedstoneAware if BundledRedstone.isAvailable && tier(stack) == Tier.Two =>
        if (WirelessRedstone.isAvailable) new component.Redstone.BundledWireless(redstone)
        else new component.Redstone.Bundled(redstone)
      case redstone: RedstoneAware =>
        if (tier(stack) == Tier.Two && WirelessRedstone.isAvailable) new component.Redstone.Wireless(redstone)
        else new component.Redstone.Simple(redstone)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(card: item.RedstoneCard) => card.tier
      case _ => Tier.One
    }

  override def providedEnvironment(stack: ItemStack): Class[_ <: Environment] =
    if (stack.getItemDamage == api.Items.get("redstoneCard1").createItemStack(1).getItemDamage)
      classOf[component.Redstone[RedstoneAware]]
    else if (BundledRedstone.isAvailable) {
      if (WirelessRedstone.isAvailable) classOf[component.Redstone.BundledWireless]
      else classOf[component.Redstone.Bundled]
    }
    else {
      if (WirelessRedstone.isAvailable) classOf[component.Redstone.Wireless]
      else classOf[component.Redstone.Simple]
    }
}
