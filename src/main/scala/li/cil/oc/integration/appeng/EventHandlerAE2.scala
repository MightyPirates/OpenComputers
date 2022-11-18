package li.cil.oc.integration.appeng

import appeng.api.implementations.items.IAEWrench
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

object EventHandlerAE2 {
  def useWrench(player: PlayerEntity, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getItemInHand(Hand.MAIN_HAND).getItem match {
      case wrench: IAEWrench => wrench.canWrench(player.getItemInHand(Hand.MAIN_HAND), player, pos)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IAEWrench]
}
