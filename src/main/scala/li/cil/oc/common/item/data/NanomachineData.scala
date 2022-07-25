package li.cil.oc.common.item.data

import li.cil.oc.common.nanomachines.ControllerImpl
import li.cil.oc.Constants
import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

class NanomachineData extends ItemData(Constants.ItemName.Nanomachines) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  def this(controller: ControllerImpl) {
    this()
    uuid = controller.uuid
    val nbt = new CompoundNBT()
    controller.configuration.saveData(nbt, forItem = true)
    configuration = Option(nbt)
  }

  var uuid = ""
  var configuration: Option[CompoundNBT] = None

  private final val UUIDTag = Settings.namespace + "uuid"
  private final val ConfigurationTag = Settings.namespace + "configuration"

  override def loadData(nbt: CompoundNBT): Unit = {
    uuid = nbt.getString(UUIDTag)
    if (nbt.contains(ConfigurationTag)) {
      configuration = Option(nbt.getCompound(ConfigurationTag))
    }
    else {
      configuration = None
    }
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    nbt.putString(UUIDTag, uuid)
    configuration.foreach(nbt.put(ConfigurationTag, _))
  }
}
