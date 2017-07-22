package li.cil.oc.integration.mfr

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import powercrystals.minefactoryreloaded.api.IMFRHammer

object EventHandlerMFR {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: IMFRHammer => true
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IMFRHammer]
}
