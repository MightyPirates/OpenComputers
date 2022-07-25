package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.api
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldReader
import net.minecraft.world.World
import net.minecraftforge.common.ToolType

object Wrench {
  val WrenchType: ToolType = ToolType.get("wrench")
}

class Wrench(props: Properties = new Properties().stacksTo(1).addToolType(Wrench.WrenchType, 1).tab(CreativeTab)) extends Item(props) with traits.SimpleItem with api.internal.Wrench {
  override def doesSneakBypassUse(stack: ItemStack, world: IWorldReader, pos: BlockPos, player: PlayerEntity): Boolean = true

  override def onItemUseFirst(player: PlayerEntity, world: World, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: Hand): ActionResultType = {
    if (world.isLoaded(pos) && world.mayInteract(player, pos)) {
      val state = world.getBlockState(pos)
      state.getBlock match {
        case block: SimpleBlock if block.rotateBlock(world, pos, side) =>
          state.neighborChanged(world, pos, Blocks.AIR, pos, false)
          player.swing(hand)
          if (!world.isClientSide) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
        case _ =>
          val updated = state.rotate(world, pos, Rotation.CLOCKWISE_90)
          if (updated != state) {
            world.setBlock(pos, updated, 3)
            player.swing(hand)
            if (!world.isClientSide) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
          }
          else super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand)
      }
    }
    else super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  def useWrenchOnBlock(player: PlayerEntity, world: World, pos: BlockPos, simulate: Boolean): Boolean = {
    if (!simulate) player.swing(Hand.MAIN_HAND)
    true
  }
}
