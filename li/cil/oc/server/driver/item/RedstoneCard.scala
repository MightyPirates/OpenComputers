package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.tileentity.Redstone
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object RedstoneCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.rs)

  override def createEnvironment(stack: ItemStack, container: AnyRef) =
    container match {
      case redstone: Redstone => new component.RedstoneCard(redstone)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card
}
