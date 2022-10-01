package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.inventory.DiskDriveMountableInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.{ActionResult, ActionResultType, Hand}
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeItem

class DiskDriveMountable(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def maxStackSize = 1

  override def use(stack: ItemStack, world: World, player: PlayerEntity) = {
    if (!world.isClientSide) player match {
      case srvPlr: ServerPlayerEntity => ContainerTypes.openDiskDriveGui(srvPlr, new DiskDriveMountableInventory {
        override def container: ItemStack = stack

        override def stillValid(player: PlayerEntity) = player == srvPlr
      })
      case _ =>
    }
    player.swing(Hand.MAIN_HAND)
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
