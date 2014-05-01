package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object LinkedCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("linkedCard"))

  override def createEnvironment(stack: ItemStack, container: TileEntity) = new component.LinkedCard()

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = 2
}
