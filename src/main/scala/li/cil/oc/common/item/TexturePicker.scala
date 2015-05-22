package li.cil.oc.common.item

import li.cil.oc.Localization
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation

class TexturePicker(val parent: Delegator) extends traits.Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    player.getEntityWorld.getBlock(position) match {
      case block: Block =>
        if (player.getEntityWorld.isRemote) {
          val model = Minecraft.getMinecraft.getBlockRendererDispatcher.getModelFromBlockState(player.getEntityWorld.getBlockState(position.toBlockPos), player.getEntityWorld, position.toBlockPos)
          if (model != null && model.getTexture != null && model.getTexture.getIconName != null) {
            player.addChatMessage(Localization.Chat.TextureName(model.getTexture.getIconName))
          }
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
