package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.{Tier, item}
import li.cil.oc.common.tileentity.traits.{BundledRedstoneAware, RedstoneAware}
import li.cil.oc.server.component
import li.cil.oc.util.mods.{BundledRedstone, WirelessRedstone}
import li.cil.oc.{Items, api}
import net.minecraft.item.ItemStack

object RedstoneCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("redstoneCard1"), api.Items.get("redstoneCard2"))

  override def createEnvironment(stack: ItemStack, container: Container) =
    container match {
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
