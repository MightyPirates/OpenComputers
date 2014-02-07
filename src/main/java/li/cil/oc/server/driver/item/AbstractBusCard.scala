package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.tileentity
import li.cil.oc.server.component
import li.cil.oc.util.mods.StargateTech2
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object AbstractBusCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.abstractBus)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) = container match {
    case computer: tileentity.Computer if StargateTech2.isAvailable => new component.AbstractBus(computer)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Card
}
