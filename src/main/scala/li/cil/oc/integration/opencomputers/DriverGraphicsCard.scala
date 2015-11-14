package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverGraphicsCard extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.GraphicsCardTier1),
    api.Items.get(Constants.ItemName.GraphicsCardTier2),
    api.Items.get(Constants.ItemName.GraphicsCardTier3))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else tier(stack) match {
      case Tier.One => new component.GraphicsCard(Tier.One)
      case Tier.Two => new component.GraphicsCard(Tier.Two)
      case Tier.Three => new component.GraphicsCard(Tier.Three)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(gpu: common.item.GraphicsCard) => gpu.gpuTier
      case _ => Tier.One
    }

  override def providedEnvironment(stack: ItemStack) = classOf[component.GraphicsCard]
}
