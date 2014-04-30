package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object WirelessNetworkCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("wlanCard"))

  override def createEnvironment(stack: ItemStack, container: TileEntity) =
    if (container != null) new component.WirelessNetworkCard(container) else null

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = 1
}
