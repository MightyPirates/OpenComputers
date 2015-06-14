package li.cil.oc.common.item

import li.cil.oc.Localization
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class TexturePicker(val parent: Delegator) extends traits.Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    player.getEntityWorld.getBlock(position) match {
      case block: Block =>
        if (player.getEntityWorld.isRemote) {
          val icon = block.getIcon(player.getEntityWorld, position.x, position.y, position.z, side)
          if (icon != null) {
            player.addChatMessage(Localization.Chat.TextureName(icon.getIconName))
          }
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
