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

object DriverDataCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.DataCardTier1),
    api.Items.get(Constants.ItemName.DataCardTier2),
    api.Items.get(Constants.ItemName.DataCardTier3))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world != null && host.world.isRemote) null
    else tier(stack) match {
      case Tier.One => new component.DataCard.Tier1()
      case Tier.Two => new component.DataCard.Tier2()
      case Tier.Three => new component.DataCard.Tier3()
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(data: common.item.DataCard) => data.tier
      case _ => Tier.One
    }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack)) tier(stack) match {
        case Tier.One => classOf[component.DataCard.Tier1]
        case Tier.Two => classOf[component.DataCard.Tier2]
        case Tier.Three => classOf[component.DataCard.Tier3]
        case _ => null
      }
      else null
  }

}
