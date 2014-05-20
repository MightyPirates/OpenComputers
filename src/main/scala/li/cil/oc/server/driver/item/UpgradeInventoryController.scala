package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import li.cil.oc.api.machine.Robot

object UpgradeInventoryController extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("inventoryControllerUpgrade"))

  override def createEnvironment(stack: ItemStack, container: component.Container) = container.tileEntity match {
    case Some(robot: TileEntity with Robot) => new component.UpgradeInventoryController(robot)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = 1
}
