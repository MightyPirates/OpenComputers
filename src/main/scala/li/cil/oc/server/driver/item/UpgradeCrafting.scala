package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object UpgradeCrafting extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.upgradeCrafting)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) = new component.UpgradeCrafting(container)

  override def slot(stack: ItemStack) = Slot.Upgrade
}
