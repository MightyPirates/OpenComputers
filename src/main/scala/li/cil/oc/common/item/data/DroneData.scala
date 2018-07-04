package li.cil.oc.common.item.data

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DroneData extends MicrocontrollerData(Constants.ItemName.Drone) {
  def this(stack: ItemStack) = {
    this()
    load(stack)
  }

  var name = ""

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    name = ItemUtils.getDisplayName(nbt).getOrElse("")
    if (Strings.isNullOrEmpty(name)) {
      name = RobotData.randomName
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    if (!Strings.isNullOrEmpty(name)) {
      ItemUtils.setDisplayName(nbt, name)
    }
  }
}
