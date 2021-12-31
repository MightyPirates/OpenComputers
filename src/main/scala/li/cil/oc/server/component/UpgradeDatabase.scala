package li.cil.oc.server.component

import java.util

import com.google.common.base.Strings
import com.google.common.hash.Hashing
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.api.prefab
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.IInventory
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.{CompressedStreamTools, JsonToNBT, NBTTagCompound}

import scala.collection.convert.WrapAsJava._

class UpgradeDatabase(val data: IInventory) extends prefab.ManagedEnvironment with internal.Database with DeviceInfo {
  override val node: Node = Network.newNode(this, Visibility.Network).
    withComponent("database").
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Object catalogue",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "iCatalogue (patent pending)",
    DeviceAttribute.Capacity -> size.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  override def size: Int = data.getSizeInventory

  override def getStackInSlot(slot: Int): ItemStack = Option(data.getStackInSlot(slot)).map(_.copy()).orNull

  override def setStackInSlot(slot: Int, stack: ItemStack): Unit = data.setInventorySlotContents(slot, stack)

  override def findStackWithHash(needle: String): Int = indexOf(needle)

  //noinspection ScalaUnusedSymbol
  @Callback(doc = "function(slot:number):table -- Get the representation of the item stack stored in the specified slot.")
  def get(context: Context, args: Arguments): Array[AnyRef] = result(data.getStackInSlot(args.checkSlot(data, 0)))

  //noinspection ScalaUnusedSymbol
  @Callback(doc = "function(slot:number):string -- Computes a hash value for the item stack in the specified slot.")
  def computeHash(context: Context, args: Arguments): Array[AnyRef] = {
    data.getStackInSlot(args.checkSlot(data, 0)) match {
      case stack: ItemStack =>
        val hash = Hashing.sha256().hashBytes(CompressedStreamTools.compress(stack))
        result(hash.toString)
      case _ => null
    }
  }

  //noinspection ScalaUnusedSymbol
  @Callback(doc = "function(hash:string):number -- Get the index of an item stack with the specified hash. Returns a negative value if no such stack was found.")
  def indexOf(context: Context, args: Arguments): Array[AnyRef] = result(indexOf(args.checkString(0), 1))

  //noinspection ScalaUnusedSymbol
  @Callback(doc = "function(slot:number):boolean -- Clears the specified slot. Returns true if there was something in the slot before.")
  def clear(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(data, 0)
    val nonEmpty = data.getStackInSlot(slot) != null
    data.setInventorySlotContents(slot, null)
    result(nonEmpty)
  }

  //noinspection ScalaUnusedSymbol
  @Callback(doc = "function(fromSlot:number, toSlot:number[, address:string]):boolean -- Copies an entry to another slot, optionally to another database. Returns true if something was overwritten.")
  def copy(context: Context, args: Arguments): Array[AnyRef] = {
    val fromSlot = args.checkSlot(data, 0)
    val entry = data.getStackInSlot(fromSlot)
    def set(inventory: IInventory) = {
      val toSlot = args.checkSlot(inventory, 1)
      val nonEmpty = inventory.getStackInSlot(toSlot) != null
      inventory.setInventorySlotContents(toSlot, entry.copy())
      result(nonEmpty)
    }
    if (args.count > 2) DatabaseAccess.withDatabase(node, args.checkString(2), database => set(database.data))
    else set(data)
  }

  //noinspection ScalaUnusedSymbol
  @Callback(doc = "function(address:string):number -- Copies the data stored in this database to another database with the specified address.")
  def clone(context: Context, args: Arguments): Array[AnyRef] = {
    DatabaseAccess.withDatabase(node, args.checkString(0), database => {
      val numberToCopy = math.min(data.getSizeInventory, database.data.getSizeInventory)
      for (slot <- 0 until numberToCopy) {
        database.data.setInventorySlotContents(slot, data.getStackInSlot(slot).copy())
      }
      context.pause(0.25)
      result(numberToCopy)
    })
  }

  //noinspection ScalaUnusedSymbol
  @Callback(doc = """function(slot:number, id:string, damage:number, nbt:string):boolean - Set an item into the specified database slot. NBT tag is expected in JSON format.""")
  def set(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(data, 0)
    val item = Item.itemRegistry.getObject(args.checkString(1)).asInstanceOf[Item]
    if (item == null)
      return result(false, "invalid item id")
    val damage = args.checkInteger(2)
    val tagJson = args.optString(3, "")
    val tag = if (Strings.isNullOrEmpty(tagJson)) null else JsonToNBT.func_150315_a(tagJson).asInstanceOf[NBTTagCompound]
    val stack = new ItemStack(item, 1, damage)
    stack.setTagCompound(tag)
    data.setInventorySlotContents(slot, stack)
    result(true)
  }

  private def indexOf(needle: String, offset: Int = 0): Int = {
    for (slot <- 0 until data.getSizeInventory) data.getStackInSlot(slot) match {
      case stack: ItemStack =>
        val hash = Hashing.sha256().hashBytes(CompressedStreamTools.compress(stack))
        if (hash.toString == needle) return slot + offset
      case _ =>
    }
    -1
  }
}
