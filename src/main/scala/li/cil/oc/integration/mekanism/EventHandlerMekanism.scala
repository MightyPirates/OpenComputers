package li.cil.oc.integration.mekanism

import mekanism.api.IMekWrench
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object EventHandlerMekanism {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: IMekWrench => wrench.canUseWrench(player, x, y, z)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IMekWrench]
}
