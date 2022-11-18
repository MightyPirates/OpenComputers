package li.cil.oc.common.item

import li.cil.oc.Localization
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.Util
import net.minecraftforge.client.model.ModelDataManager
import net.minecraftforge.common.extensions.IForgeItem

class TexturePicker(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def onItemUse(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    player.level.getBlock(position) match {
      case block: Block =>
        if (player.level.isClientSide) {
          val pos = position.toBlockPos
          val model = Minecraft.getInstance.getBlockRenderer.getBlockModel(player.level.getBlockState(pos))
          val particle = if (model != null) model.getParticleTexture(ModelDataManager.getModelData(player.level, pos)) else null
          if (particle != null && particle.getName != null) {
            player.sendMessage(Localization.Chat.TextureName(particle.getName.toString), Util.NIL_UUID)
          }
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
