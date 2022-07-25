package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader

class EEPROM(val parent: Delegator) extends traits.Delegate {
  override def displayName(stack: ItemStack): Option[String] = {
    if (stack.hasTag) {
      val tag = stack.getTag
      if (tag.contains(Settings.namespace + "data")) {
        val data = tag.getCompound(Settings.namespace + "data")
        if (data.contains(Settings.namespace + "label")) {
          return Some(data.getString(Settings.namespace + "label"))
        }
      }
    }
    super.displayName(stack)
  }

  override def doesSneakBypassUse(world: IBlockReader, pos: BlockPos, player: PlayerEntity): Boolean = true
}
