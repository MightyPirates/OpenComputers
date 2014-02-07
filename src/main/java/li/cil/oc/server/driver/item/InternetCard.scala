package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.Context
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object InternetCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.internet)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) = container match {
    case context: Context => new component.InternetCard(context)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = 1
}
