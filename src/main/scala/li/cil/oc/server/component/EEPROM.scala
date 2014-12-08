package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import net.minecraft.nbt.NBTTagCompound

class EEPROM extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("eeprom", Visibility.Network).
    withConnector().
    create()

  var data = Array.empty[Byte]

  var label = "EEPROM"

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():string -- Get the currently stored byte array.""")
  def get(context: Context, args: Arguments): Array[AnyRef] = result(data)

  @Callback(doc = """function(data:string) -- Overwrite the currently stored byte array.""")
  def set(context: Context, args: Arguments): Array[AnyRef] = {
    if (!node.tryChangeBuffer(-Settings.get.eepromWriteCost)) {
      return result(Unit, "not enough energy")
    }
    val newData = args.checkByteArray(0)
    if (newData.length > 4 * 1024) throw new IllegalArgumentException("not enough space")
    data = newData
    context.pause(2) // deliberately slow to discourage use as normal storage medium
    null
  }

  @Callback(direct = true, doc = """function():string -- Get the label of the EEPROM.""")
  def getLabel(context: Context, args: Arguments): Array[AnyRef] = result(label)

  @Callback(doc = """function(data:string) -- Set the label of the EEPROM.""")
  def setLabel(context: Context, args: Arguments): Array[AnyRef] = {
    label = args.checkString(0).take(16)
    null
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    data = nbt.getByteArray(Settings.namespace + "eeprom")
    if (nbt.hasKey(Settings.namespace + "label")) {
      label = nbt.getString(Settings.namespace + "label")
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setByteArray(Settings.namespace + "eeprom", data)
    nbt.setString(Settings.namespace + "label", label)
  }
}
