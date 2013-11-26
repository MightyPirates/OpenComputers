package li.cil.oc.server.driver.item

import cpw.mods.fml.common.Loader
import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.tileentity.{BundledRedstone, Redstone}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object RedstoneCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.rs)

  override def createEnvironment(stack: ItemStack, container: AnyRef) =
    container match {
      case redstone: BundledRedstone if isBundledRedstoneModAvailable => new component.BundledRedstoneCard(redstone)
      case redstone: Redstone => new component.RedstoneCard(redstone)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card

  private def isBundledRedstoneModAvailable = Loader.isModLoaded("RedLogic") || Loader.isModLoaded("MineFactoryReloaded")
}
