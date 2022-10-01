package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IWorldReader
import net.minecraftforge.common.extensions.IForgeItem

class EEPROM(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def getName(stack: ItemStack): ITextComponent = {
    if (stack.hasTag) {
      val tag = stack.getTag
      if (tag.contains(Settings.namespace + "data")) {
        val data = tag.getCompound(Settings.namespace + "data")
        if (data.contains(Settings.namespace + "label")) {
          return new StringTextComponent(data.getString(Settings.namespace + "label"))
        }
      }
    }
    super.getName(stack)
  }

  override def doesSneakBypassUse(stack: ItemStack, world: IWorldReader, pos: BlockPos, player: PlayerEntity): Boolean = true
}
