package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object Memory extends Item with driver.Memory {
  def amount(stack: ItemStack) = Items.multi.subItem(stack) match {
    case Some(memory: item.Memory) => memory.kiloBytes * 1024
    case _ => 0
  }

  def worksWith(stack: ItemStack) = isOneOf(stack, Items.ram3, Items.ram1, Items.ram2)

  def createEnvironment(stack: ItemStack, container: MCTileEntity) = null

  def slot(stack: ItemStack) = Slot.Memory

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(memory: item.Memory) => memory.tier
      case _ => 0
    }
}
