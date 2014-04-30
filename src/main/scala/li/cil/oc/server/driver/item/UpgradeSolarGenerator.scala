package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object UpgradeSolarGenerator extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("solarGeneratorUpgrade"))

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) = new component.UpgradeSolarGenerator(container)

  override def slot(stack: ItemStack) = Slot.Upgrade
}
