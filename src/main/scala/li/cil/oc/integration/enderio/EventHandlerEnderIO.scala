package li.cil.oc.integration.enderio

import crazypants.enderio.tool.ITool
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object EventHandlerEnderIO {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: ITool =>
        if (changeDurability) {
          wrench.used(player.getHeldItem, player, x, y, z)
          true
        }
        else wrench.canUse(player.getHeldItem, player, x, y, z)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[ITool]
}
