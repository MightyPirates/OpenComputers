package li.cil.oc.integration.railcraft

import mods.railcraft.api.core.items.IToolCrowbar
import net.minecraft.entity.player.EntityPlayer

object EventHandlerRailcraft {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getCurrentEquippedItem.getItem match {
      case wrench: IToolCrowbar =>
        if (changeDurability) {
          wrench.onWhack(player, player.getHeldItem, x, y, z)
          true
        }
        else wrench.canWhack(player, player.getHeldItem, x, y, z)
      case _ => false
    }
  }
}
