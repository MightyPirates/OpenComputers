package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeSolarGenerator extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("solarGeneratorUpgrade"))

  override def createEnvironment(stack: ItemStack, container: component.Container) =
    container.tileEntity match {
      case Some(tileEntity) => new component.UpgradeSolarGenerator(tileEntity)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three
}
