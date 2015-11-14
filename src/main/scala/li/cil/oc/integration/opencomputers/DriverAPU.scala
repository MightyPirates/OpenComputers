package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common
import li.cil.oc.common.Tier
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverAPU extends DriverCPU with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.APUTier1),
    api.Items.get(Constants.ItemName.APUTier2),
    api.Items.get(Constants.ItemName.APUCreative))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else gpuTier(stack) match {
      case Tier.One => new component.GraphicsCard(Tier.One)
      case Tier.Two => new component.GraphicsCard(Tier.Two)
      case Tier.Three => new component.GraphicsCard(Tier.Three)
      case _ => null
    }

  override def cpuTier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(apu: common.item.APU) => apu.cpuTier
      case _ => Tier.One
    }

  def gpuTier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(apu: common.item.APU) => apu.gpuTier
      case _ => Tier.One
    }

  override def providedEnvironment(stack: ItemStack) = classOf[component.GraphicsCard]
}
