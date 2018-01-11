package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverWirelessNetworkCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.WirelessNetworkCardTier1),
    api.Items.get(Constants.ItemName.WirelessNetworkCardTier2))
    
  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world != null && host.world.isRemote) null
    else tier(stack) match {
      case Tier.One => new component.WirelessNetworkCard.Tier1(host)
      case Tier.Two => new component.WirelessNetworkCard.Tier2(host)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(card: common.item.WirelessNetworkCard) => card.tier
      case _ => Tier.One
    }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack)) tier(stack) match {
        case Tier.One => classOf[component.WirelessNetworkCard.Tier1]
        case Tier.Two => classOf[component.WirelessNetworkCard.Tier2]
        case _ => null
      }
      else null
  }

}
