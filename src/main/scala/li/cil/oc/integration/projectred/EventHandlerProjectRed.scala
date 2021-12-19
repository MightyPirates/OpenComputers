package li.cil.oc.integration.projectred

import mrtjp.projectred.api.IScrewdriver
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object EventHandlerProjectRed {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: IScrewdriver =>
        if (changeDurability) {
          wrench.damageScrewdriver(player, new ItemStack(player.getHeldItem.getItem))
          true
        }
        else true
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IScrewdriver]
}
