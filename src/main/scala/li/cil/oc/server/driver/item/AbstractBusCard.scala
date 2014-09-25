package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import li.cil.oc.util.mods.Mods
import net.minecraft.item.ItemStack
import stargatetech2.api.bus.IBusDevice

object AbstractBusCard extends Item {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("abstractBusCard"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
     super.worksWith(stack, host) && isComputer(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = if (Mods.StargateTech2.isAvailable) host match {
    case device: IBusDevice => new component.AbstractBusCard(device)
    case _ => null
  }
  else null

  override def slot(stack: ItemStack) = Slot.Card
}
