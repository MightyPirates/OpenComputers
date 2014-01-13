package li.cil.oc.server.driver.item

import cpw.mods.fml.common.Loader
import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.tileentity
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object AbstractBusCard extends Item {
  def worksWith(stack: ItemStack) = isOneOf(stack, Items.abstractBus)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) = container match {
    case computer: tileentity.Computer if Loader.isModLoaded("StargateTech2") => new component.AbstractBus(computer)
    case _ => null
  }

  def slot(stack: ItemStack) = Slot.Card
}
