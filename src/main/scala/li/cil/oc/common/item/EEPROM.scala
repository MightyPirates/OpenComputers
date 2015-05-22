package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class EEPROM extends traits.SimpleItem {
  override def doesSneakBypassUse(world: World, x: Int, y: Int, z: Int, player: EntityPlayer): Boolean = true

  override def getItemStackDisplayName(stack: ItemStack): String = {
    if (stack.hasTagCompound) {
      val tag = stack.getTagCompound
      if (tag.hasKey(Settings.namespace + "data")) {
        val data = tag.getCompoundTag(Settings.namespace + "data")
        if (data.hasKey(Settings.namespace + "label")) {
          return data.getString(Settings.namespace + "label")
        }
      }
    }
    super.getItemStackDisplayName(stack)
  }
}
