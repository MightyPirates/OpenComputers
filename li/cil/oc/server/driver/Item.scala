package li.cil.oc.server.driver

import li.cil.oc.api
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends api.driver.Item {
  def nbt(item: ItemStack) = {
    if (!item.hasTagCompound)
      item.setTagCompound(new NBTTagCompound())
    val nbt = item.getTagCompound
    if (!nbt.hasKey("oc.node")) {
      nbt.setCompoundTag("oc.node", new NBTTagCompound())
    }
    nbt.getCompoundTag("oc.node")
  }
}
