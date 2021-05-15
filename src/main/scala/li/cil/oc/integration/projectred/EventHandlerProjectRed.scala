package li.cil.oc.integration.projectred

import mrtjp.projectred.api.IScrewdriver
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

object EventHandlerProjectRed {
  def useWrench(player: EntityPlayer, pos: BlockPos, changeDurability: Boolean): Boolean = {
    val stack = player.getHeldItemMainhand
    stack.getItem match {
      case wrench: IScrewdriver =>
        if (changeDurability) {
          wrench.damageScrewdriver(player, stack)
          true
        }
        else true
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IScrewdriver]
}
