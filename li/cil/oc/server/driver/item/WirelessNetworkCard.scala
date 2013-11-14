package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object WirelessNetworkCard extends Item {
  def worksWith(item: ItemStack) = isOneOf(item, Items.wlan)

  override def createEnvironment(item: ItemStack, container: AnyRef) =
    container match {
      case t: TileEntity => new component.WirelessNetworkCard(t)
      case _ => null
    }

  def slot(item: ItemStack) = Slot.Card
}
