package li.cil.oc.server.driver.item

import li.cil.oc.{api, Items, common}
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object GraphicsCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("graphicsCard1"), api.Items.get("graphicsCard2"), api.Items.get("graphicsCard3"))

  override def createEnvironment(stack: ItemStack, container: component.Container) =
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