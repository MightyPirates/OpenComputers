package li.cil.oc.integration.cofh.item

import cofh.api.item.IToolHammer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

object EventHandlerCoFH {
  def useWrench(player: EntityPlayer, pos: BlockPos, changeDurability: Boolean): Boolean = {
    player.getHeldItemMainhand.getItem match {
      case wrench: IToolHammer =>
        if (changeDurability) {
          wrench.toolUsed(player.getHeldItemMainhand, player, pos)
          true
        }
        else wrench.isUsable(player.getHeldItemMainhand, player, pos)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IToolHammer]
}
