package li.cil.oc.integration.stargatetech2

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.integration.Mods
import li.cil.oc.integration.opencomputers.Item
import lordfokas.stargatetech2.api.bus.IBusDevice
import net.minecraft.item.ItemStack

object DriverAbstractBusCard extends Item with HostAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get(Constants.ItemName.AbstractBusCard))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && (isComputer(host) || isRobot(host) || isServer(host) || isMicrocontroller(host))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = if (Mods.StargateTech2.isAvailable) host match {
    case device: IBusDevice => new AbstractBusCard(device)
    case _ => null
  }
  else null

  override def slot(stack: ItemStack) = Slot.Card

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[AbstractBusCard]
      else null
  }

}
