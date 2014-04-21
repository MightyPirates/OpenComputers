package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.util.mods.Mods
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import stargatetech2.api.bus.IBusDevice

object AbstractBusCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.abstractBus)

  override def createEnvironment(stack: ItemStack, container: TileEntity) = if (Mods.StargateTech2.isAvailable) container match {
    case device: IBusDevice => new component.AbstractBus(device)
    case _ => null
  }
  else null

  override def slot(stack: ItemStack) = Slot.Card
}
