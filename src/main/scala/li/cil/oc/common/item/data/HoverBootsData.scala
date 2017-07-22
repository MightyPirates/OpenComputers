package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class HoverBootsData extends ItemData(Constants.ItemName.HoverBoots) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var charge = 0.0

  override def load(nbt: NBTTagCompound) {
    charge = nbt.getDouble(Settings.namespace + "charge")
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setDouble(Settings.namespace + "charge", charge)
  }
}
