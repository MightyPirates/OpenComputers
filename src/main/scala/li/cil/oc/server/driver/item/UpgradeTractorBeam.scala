package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.api.machine.Robot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeTractorBeam extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("tractorBeamUpgrade"))

  override def createEnvironment(stack: ItemStack, container: Container) = container match {
    case robot: Robot => new component.UpgradeTractorBeam(robot)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three
}
