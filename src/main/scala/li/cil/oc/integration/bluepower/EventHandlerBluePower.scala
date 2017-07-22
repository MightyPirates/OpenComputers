package li.cil.oc.integration.bluepower

import com.bluepowermod.api.misc.IScrewdriver
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object EventHandlerBluePower {
  def useWrench(player: EntityPlayer, x: Int, y: Int, z: Int, changeDurability: Boolean): Boolean = {
    player.getHeldItem.getItem match {
      case wrench: IScrewdriver => wrench.damage(player.getHeldItem, 0, player, changeDurability)
      case _ => false
    }
  }

  def isWrench(stack: ItemStack): Boolean = stack.getItem.isInstanceOf[IScrewdriver]
}
