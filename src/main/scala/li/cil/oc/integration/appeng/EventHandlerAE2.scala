package li.cil.oc.integration.appeng

import appeng.api.implementations.items.IAEWrench
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos

object EventHandlerAE2 {
  def useWrench(player: EntityPlayer, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getHeldItem(EnumHand.MAIN_HAND).getItem match {
      case wrench: IAEWrench => wrench.canWrench(player.getHeldItem(EnumHand.MAIN_HAND), player, pos)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IAEWrench]
}
