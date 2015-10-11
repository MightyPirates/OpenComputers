package li.cil.oc.common.item.data

import li.cil.oc.common.nanomachines.ControllerImpl
import li.cil.oc.{Constants, Settings}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class NanomachineData extends ItemData(Constants.ItemName.Nanomachines) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  def this(controller: ControllerImpl) {
    this()
    uuid = controller.uuid
    val nbt = new NBTTagCompound()
    controller.configuration.save(nbt, forItem = true)
    configuration = Option(nbt)
  }

  var uuid = ""
  var configuration: Option[NBTTagCompound] = None

  override def load(nbt: NBTTagCompound): Unit = {
    uuid = nbt.getString(Settings.namespace + "uuid")
    if (nbt.hasKey(Settings.namespace + "configuration")) {
      configuration = Option(nbt.getCompoundTag(Settings.namespace + "configuration"))
    }
    else {
      configuration = None
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    nbt.setString(Settings.namespace + "uuid", uuid)
    configuration.foreach(nbt.setTag(Settings.namespace + "configuration", _))
  }
}
