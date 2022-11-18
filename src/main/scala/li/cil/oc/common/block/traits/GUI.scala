package li.cil.oc.common.block.traits

import li.cil.oc.OpenComputers
import li.cil.oc.common.block.SimpleBlock
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

trait GUI extends SimpleBlock {
  def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos)

  // This gets forwarded to the vanilla PlayerEntity.openMenu call which doesn't support extra data.
  override def getMenuProvider(state: BlockState, world: World, pos: BlockPos): INamedContainerProvider = null

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.isCrouching) {
      player match {
        case srvPlr: ServerPlayerEntity if !world.isClientSide => openGui(srvPlr, world, pos)
        case _ =>
      }
      true
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }
}
