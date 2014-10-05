package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.init.Items
import li.cil.oc.server.component
import li.cil.oc.util.mods.BundledRedstone
import li.cil.oc.util.mods.WirelessRedstone
import net.minecraft.item.ItemStack

object RedstoneCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("redstoneCard1"), api.Items.get("redstoneCard2"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    super.worksWith(stack, host) && isComputer(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    host match {
      case redstone: BundledRedstoneAware if BundledRedstone.isAvailable && tier(stack) == Tier.Two =>
        if (WirelessRedstone.isAvailable) new component.Redstone[BundledRedstoneAware](redstone) with component.RedstoneBundled with component.RedstoneWireless
        else new component.Redstone[BundledRedstoneAware](redstone) with component.RedstoneBundled
      case redstone: RedstoneAware =>
        if (tier(stack) == Tier.Two && WirelessRedstone.isAvailable) new component.Redstone[RedstoneAware](redstone) with component.RedstoneWireless
        else new component.Redstone[RedstoneAware](redstone)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(card: item.RedstoneCard) => card.tier
      case _ => Tier.One
    }
}
