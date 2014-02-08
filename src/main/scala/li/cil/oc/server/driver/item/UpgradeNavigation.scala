package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.server.driver.Registry
import li.cil.oc.{Settings, Items}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object UpgradeNavigation extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.upgradeNavigation)

  override def createEnvironment(stack: ItemStack, container: TileEntity) = {
    val nbt = Registry.itemDriverFor(stack) match {
      case Some(driver) => driver.dataTag(stack)
      case _ => null
    }
    val x = if (nbt.hasKey(Settings.namespace + "xCenter")) nbt.getInteger(Settings.namespace + "xCenter") else container.xCoord
    val z = if (nbt.hasKey(Settings.namespace + "zCenter")) nbt.getInteger(Settings.namespace + "zCenter") else container.zCoord
    val size = if (nbt.hasKey(Settings.namespace + "scale")) nbt.getInteger(Settings.namespace + "scale") else 512
    new component.UpgradeNavigation(container, x, z, size)
  }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
