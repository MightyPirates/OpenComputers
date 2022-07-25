package li.cil.oc.integration.projectred

import mrtjp.projectred.api.IScrewdriver
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

object EventHandlerProjectRed {
  def useWrench(player: PlayerEntity, pos: BlockPos, changeDurability: Boolean): Boolean = {
    val stack = player.getItemInHand(Hand.MAIN_HAND)
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
