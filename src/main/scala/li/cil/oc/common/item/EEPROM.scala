package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

class EEPROM extends SimpleItem {
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
