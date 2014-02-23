package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}

object GraphicsCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.gpu1, Items.gpu2, Items.gpu3)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) =
    Items.multi.subItem(stack) match {
      case Some(gpu: common.item.GraphicsCard) => gpu.tier match {
        case 0 => new component.GraphicsCard.Tier1()
        case 1 => new component.GraphicsCard.Tier2()
        case 2 => new component.GraphicsCard.Tier3()
        case _ => null
      }
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(gpu: common.item.GraphicsCard) => gpu.tier
      case _ => 0
    }
}