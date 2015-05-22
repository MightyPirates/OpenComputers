package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

class EEPROM(val parent: Delegator) extends traits.Delegate {
  override def displayName(stack: ItemStack): Option[String] = {
    if (stack.hasTagCompound) {
      val tag = stack.getTagCompound
      if (tag.hasKey(Settings.namespace + "data")) {
        val data = tag.getCompoundTag(Settings.namespace + "data")
        if (data.hasKey(Settings.namespace + "label")) {
          return Some(data.getString(Settings.namespace + "label"))
        }
      }
    }
    super.displayName(stack)
  }
}
