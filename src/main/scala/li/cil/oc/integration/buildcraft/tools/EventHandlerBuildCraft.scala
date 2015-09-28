package li.cil.oc.integration.buildcraft.tools

import buildcraft.api.tools.IToolWrench
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object EventHandlerBuildCraft {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: IToolWrench =>
        if (changeDurability) {
          wrench.wrenchUsed(player, x, y, z)
          true
        }
        else wrench.canWrench(player, x, y, z)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[buildcraft.api.tools.IToolWrench]
}
