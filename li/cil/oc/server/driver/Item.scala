package li.cil.oc.server.driver

import java.io.InputStream
import li.cil.oc.api
import li.cil.oc.api.network.Node
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends api.driver.Item {
  def api: InputStream = null

  def nbt(item: ItemStack) = {
    if (!item.hasTagCompound)
      item.setTagCompound(new NBTTagCompound())
    val nbt = item.getTagCompound
    if (!nbt.hasKey("oc.node")) {
      nbt.setCompoundTag("oc.node", new NBTTagCompound())
    }
    nbt.getCompoundTag("oc.node")
  }

  def node(item: ItemStack): Node = null
}
