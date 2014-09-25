package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeChunkloader extends Item {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("chunkloaderUpgrade"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    super.worksWith(stack, host) && isRobot(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.UpgradeChunkloader(host)

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three
}
