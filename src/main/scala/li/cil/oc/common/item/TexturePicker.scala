package li.cil.oc.common.item

import li.cil.oc.Localization
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

class TexturePicker(val parent: Delegator) extends traits.Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    player.getEntityWorld.getBlock(position) match {
      case block: Block =>
        if (player.getEntityWorld.isRemote) {
          val model = Minecraft.getMinecraft.getBlockRendererDispatcher.getModelForState(player.getEntityWorld.getBlockState(position.toBlockPos))
          if (model != null && model.getParticleTexture != null && model.getParticleTexture.getIconName != null) {
            player.sendMessage(Localization.Chat.TextureName(model.getParticleTexture.getIconName))
          }
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
