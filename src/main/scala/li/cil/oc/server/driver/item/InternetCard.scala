package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Host
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object InternetCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("internetCard"))

  override def createEnvironment(stack: ItemStack, host: Host) = new component.InternetCard()

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = Tier.Two
}
