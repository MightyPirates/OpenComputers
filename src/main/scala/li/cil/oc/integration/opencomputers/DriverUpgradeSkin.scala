package li.cil.oc.integration.opencomputers

import li.cil.oc.{Constants, api}
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.{Slot, Tier, item}
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeSkin extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.SkinUpgradeTier1),
    api.Items.get(Constants.ItemName.SkinUpgradeTier2),
    api.Items.get(Constants.ItemName.SkinUpgradeTier3))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world != null && host.world.isRemote) null
    else host match {
      case host: EnvironmentHost with Robot => new component.UpgradeSkin.Robot(host, tier(stack))
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Delegator.subItem(stack) match {
    case Some(skin: item.UpgradeSkin) => skin.tier
    case _ => Tier.One
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeSkin.Robot]
      else null
  }

}
