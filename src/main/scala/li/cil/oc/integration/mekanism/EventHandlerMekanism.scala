package li.cil.oc.integration.mekanism

import mekanism.api.IMekWrench
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos

object EventHandlerMekanism {
  def useWrench(player: EntityPlayer, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getHeldItem(EnumHand.MAIN_HAND).getItem match {
      case wrench: IMekWrench => wrench.canUseWrench(player.getHeldItem(EnumHand.MAIN_HAND), player, pos)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IMekWrench]
}
