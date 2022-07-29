package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverAPU extends DriverCPU with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.APUTier1),
    api.Items.get(Constants.ItemName.APUTier2),
    api.Items.get(Constants.ItemName.APUCreative))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world != null && host.world.isClientSide) null
    else gpuTier(stack) match {
      case Tier.One => new component.APU(Tier.One)
      case Tier.Two => new component.APU(Tier.Two)
      case Tier.Three => new component.APU(Tier.Three)
      case _ => null
    }

  override def cpuTier(stack: ItemStack) =
    stack.getItem match {
      case apu: common.item.APU => apu.cpuTier
      case _ => Tier.One
    }

  def gpuTier(stack: ItemStack) =
    stack.getItem match {
      case apu: common.item.APU => apu.gpuTier
      case _ => Tier.One
    }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.GraphicsCard]
      else null
  }

}
