package li.cil.oc.integration.cofh.item

import cofh.api.item.IToolHammer
import net.minecraft.entity.player.EntityPlayer

object EventHandlerCoFH {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getCurrentEquippedItem.getItem match {
      case wrench: IToolHammer =>
        if (changeDurability) {
          wrench.toolUsed(player.getHeldItem, player, x, y, z)
          true
        }
        else wrench.isUsable(player.getHeldItem, player, x, y, z)
      case _ => false
    }
  }
}
