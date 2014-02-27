package li.cil.oc.util.mods

import buildcraft.api.tools.IToolWrench
import cpw.mods.fml.common.ModAPIManager
import net.minecraft.entity.player.EntityPlayer

object BuildCraft {
  def holdsApplicableWrench(player: EntityPlayer, x: Int, y: Int, z: Int) =
    ModAPIManager.INSTANCE.hasAPI("BuildCraftAPI|tools") &&
      player.getCurrentEquippedItem != null &&
      (player.getCurrentEquippedItem.getItem match {
        case wrench: IToolWrench => wrench.canWrench(player, x, y, z)
        case _ => false
      })

  def wrenchUsed(player: EntityPlayer, x: Int, y: Int, z: Int) =
    ModAPIManager.INSTANCE.hasAPI("BuildCraftAPI|tools") &&
      player.getCurrentEquippedItem != null &&
      (player.getCurrentEquippedItem.getItem match {
        case wrench: IToolWrench if wrench.canWrench(player, x, y, z) =>
          wrench.wrenchUsed(player, x, y, z)
          true
        case _ => false
      })
}
