package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object Generator extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.generator)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) = new component.Generator(container)

  override def slot(stack: ItemStack) = Slot.Upgrade
}
