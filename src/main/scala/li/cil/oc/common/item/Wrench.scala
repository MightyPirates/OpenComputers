package li.cil.oc.common.item

import li.cil.oc.api
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Wrench extends traits.SimpleItem with api.internal.Wrench {
  setHarvestLevel("wrench", 1)
  setMaxStackSize(1)

  override def doesSneakBypassUse(stack: ItemStack, world: IBlockAccess, pos: BlockPos, player: EntityPlayer): Boolean = true

  override def onItemUseFirst(player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, hand: EnumHand): EnumActionResult = {
    if (world.isBlockLoaded(pos) && world.isBlockModifiable(player, pos)) world.getBlockState(pos).getBlock match {
      case block: Block if block.rotateBlock(world, pos, side) =>
        block.neighborChanged(world.getBlockState(pos), world, pos, Blocks.AIR, pos)
        player.swingArm(hand)
        if (!world.isRemote) EnumActionResult.SUCCESS else EnumActionResult.PASS
      case _ =>
        super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand)
    }
    else super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  def useWrenchOnBlock(player: EntityPlayer, world: World, pos: BlockPos, simulate: Boolean): Boolean = {
    if (!simulate) player.swingArm(EnumHand.MAIN_HAND)
    true
  }
}
