package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.machine.Robot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object UpgradeBlockPlacerAir extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.upgradeBlockPlacerAir)

  override def createEnvironment(stack: ItemStack, container: TileEntity) =
    container match {
      case robot: Robot => new component.UpgradeBlockPlacerAir(robot)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
