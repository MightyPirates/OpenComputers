package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.{Environment, NodeContainerItem}
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.common.component.TerminalServer
import li.cil.oc.util.ExtendedInventory._
import net.minecraft.item.ItemStack

object DriverTerminalServer extends Item with HostAware {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.TerminalServer))

  override def createEnvironment(stack: ItemStack, host: Location): NodeContainerItem = host match {
    case rack: api.internal.Rack => new TerminalServer(rack, rack.indexOf(stack))
    case _ => null
  }

  override def slot(stack: ItemStack): String = Slot.RackMountable
}
