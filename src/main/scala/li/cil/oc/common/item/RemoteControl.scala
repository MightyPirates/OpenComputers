package li.cil.oc.common.item

import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

class RemoteControl(val parent: Delegator) extends traits.Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
  }
}
