package li.cil.oc.common.item.data

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.api
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DroneData extends MicrocontrollerData {
  def this(stack: ItemStack) = {
    this()
    load(stack)
  }

  var name = ""

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    if (nbt.hasKey("display") && nbt.getCompoundTag("display").hasKey("Name")) {
      name = nbt.getCompoundTag("display").getString("Name")
    }
    if (Strings.isNullOrEmpty(name)) {
      name = RobotData.randomName
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    if (!Strings.isNullOrEmpty(name)) {
      if (!nbt.hasKey("display")) {
        nbt.setTag("display", new NBTTagCompound())
      }
      nbt.getCompoundTag("display").setString("Name", name)
    }
  }

  override def createItemStack(): ItemStack = {
    val stack = api.Items.get(Constants.ItemName.Drone).createItemStack(1)
    save(stack)
    stack
  }
}
