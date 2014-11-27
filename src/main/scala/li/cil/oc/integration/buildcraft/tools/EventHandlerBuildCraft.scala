package li.cil.oc.integration.buildcraft.tools

import buildcraft.api.tools.IToolWrench
import net.minecraft.entity.player.EntityPlayer

object EventHandlerBuildCraft {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getCurrentEquippedItem.getItem match {
      case wrench: IToolWrench =>
        if (changeDurability) {
          wrench.wrenchUsed(player, x, y, z)
          true
        }
        else wrench.canWrench(player, x, y, z)
      case _ => false
    }
  }
}
