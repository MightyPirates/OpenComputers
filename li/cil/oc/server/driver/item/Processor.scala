package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object Processor extends Item {
  def worksWith(stack: ItemStack) = isOneOf(stack, Items.cpu0, Items.cpu1, Items.cpu2)

  def createEnvironment(stack: ItemStack, container: TileEntity) = null

  def slot(stack: ItemStack) = Slot.Processor

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => cpu.tier
      case _ => 0
    }
}
