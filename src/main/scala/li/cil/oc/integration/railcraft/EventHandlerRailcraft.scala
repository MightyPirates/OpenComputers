package li.cil.oc.integration.railcraft

import mods.railcraft.api.items.IToolCrowbar
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos

object EventHandlerRailcraft {
  def useWrench(player: EntityPlayer, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getHeldItemMainhand.getItem match {
      case wrench: IToolCrowbar =>
        if (changeDurability) {
          wrench.onWhack(player, EnumHand.MAIN_HAND, player.getHeldItemMainhand, pos)
          true
        }
        else wrench.canWhack(player, EnumHand.MAIN_HAND, player.getHeldItemMainhand, pos)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IToolCrowbar]
}
