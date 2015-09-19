package li.cil.oc.integration.cofh.item

import cofh.api.item.IToolHammer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object EventHandlerCoFH {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: IToolHammer =>
        if (changeDurability) {
          wrench.toolUsed(player.getHeldItem, player, x, y, z)
          true
        }
        else wrench.isUsable(player.getHeldItem, player, x, y, z)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IToolHammer]
}
