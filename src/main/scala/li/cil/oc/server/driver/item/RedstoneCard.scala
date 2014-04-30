package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.util.mods.BundledRedstone
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}
import li.cil.oc.common.tileentity.traits.{RedstoneAware, BundledRedstoneAware}

object RedstoneCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("redstoneCard"))

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) =
    container match {
      case redstone: BundledRedstoneAware if BundledRedstone.isAvailable => new component.BundledRedstone(redstone)
      case redstone: RedstoneAware => new component.Redstone(redstone)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Card
}
