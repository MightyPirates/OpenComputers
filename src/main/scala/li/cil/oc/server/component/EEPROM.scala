package li.cil.oc.server.component

import java.util

import com.google.common.hash.Hashing
import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._

class EEPROM extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("eeprom", Visibility.Neighbors).
    withConnector().
    create()

  var codeData = Array.empty[Byte]

  var volatileData = Array.empty[Byte]

  var readonly = false

  var label = "EEPROM"

  def checksum = Hashing.crc32().hashBytes(codeData).toString

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Memory,
    DeviceAttribute.Description -> "EEPROM",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "FlashStick2k",
    DeviceAttribute.Capacity -> Settings.get.eepromSize.toString,
    DeviceAttribute.Size -> Settings.get.eepromSize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():string -- Get the currently stored byte array.""")
  def get(context: Context, args: Arguments): Array[AnyRef] = result(codeData)

  @Callback(doc = """function(data:string) -- Overwrite the currently stored byte array.""")
  def set(context: Context, args: Arguments): Array[AnyRef] = {
    if (readonly) {
      return result(Unit, "storage is readonly")
    }
    if (!node.tryChangeBuffer(-Settings.get.eepromWriteCost)) {
      return result(Unit, "not enough energy")
    }
    val newData = args.optByteArray(0, Array.empty[Byte])
    if (newData.length > Settings.get.eepromSize) throw new IllegalArgumentException("not enough space")
    codeData = newData
    context.pause(2) // deliberately slow to discourage use as normal storage medium
    null
  }

  @Callback(direct = true, doc = """function():string -- Get the label of the EEPROM.""")
  def getLabel(context: Context, args: Arguments): Array[AnyRef] = result(label)

  @Callback(doc = """function(data:string):string -- Set the label of the EEPROM.""")
  def setLabel(context: Context, args: Arguments): Array[AnyRef] = {
    if (readonly) {
      return result(Unit, "storage is readonly")
    }
    label = args.optString(0, "EEPROM").trim.take(24)
    if (label.length == 0) label = "EEPROM"
    result(label)
  }

  @Callback(direct = true, doc = """function():number -- Get the storage capacity of this EEPROM.""")
  def getSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.eepromSize)

  @Callback(direct = true, doc = """function():string -- Get the checksum of the data on this EEPROM.""")
  def getChecksum(context: Context, args: Arguments): Array[AnyRef] = result(checksum)

  @Callback(direct = true, doc = """function(checksum:string):boolean -- Make this EEPROM readonly if it isn't already. This process cannot be reversed!""")
  def makeReadonly(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.checkString(0) == checksum) {
      readonly = true
      result(true)
    }
    else result(Unit, "incorrect checksum")
  }

  @Callback(direct = true, doc = """function():number -- Get the storage capacity of this EEPROM.""")
  def getDataSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.eepromDataSize)

  @Callback(direct = true, doc = """function():string -- Get the currently stored byte array.""")
  def getData(context: Context, args: Arguments): Array[AnyRef] = result(volatileData)

  @Callback(doc = """function(data:string) -- Overwrite the currently stored byte array.""")
  def setData(context: Context, args: Arguments): Array[AnyRef] = {
    if (!node.tryChangeBuffer(-Settings.get.eepromWriteCost)) {
      return result(Unit, "not enough energy")
    }
    val newData = args.optByteArray(0, Array.empty[Byte])
    if (newData.length > Settings.get.eepromDataSize) throw new IllegalArgumentException("not enough space")
    volatileData = newData
    context.pause(1) // deliberately slow to discourage use as normal storage medium
    null
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    codeData = nbt.getByteArray(Settings.namespace + "eeprom")
    if (nbt.hasKey(Settings.namespace + "label")) {
      label = nbt.getString(Settings.namespace + "label")
    }
    readonly = nbt.getBoolean(Settings.namespace + "readonly")
    volatileData = nbt.getByteArray(Settings.namespace + "userdata")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setByteArray(Settings.namespace + "eeprom", codeData)
    nbt.setString(Settings.namespace + "label", label)
    nbt.setBoolean(Settings.namespace + "readonly", readonly)
    nbt.setByteArray(Settings.namespace + "userdata", volatileData)
  }
}
