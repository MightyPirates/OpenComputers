package li.cil.oc.server.component

import com.google.common.hash.Hashing
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{Component, Visibility}
import li.cil.oc.api.{Network, internal, prefab}
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools

class UpgradeDatabase(val data: IInventory) extends prefab.ManagedEnvironment with internal.Database {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("database").
    create()

  override def size() = data.getSizeInventory

  override def getStackInSlot(slot: Int) = Option(data.getStackInSlot(slot)).map(_.copy()).orNull

  override def findStackWithHash(needle: String) = {
    val slot = indexOf(needle)
    if (slot >= 0) data.getStackInSlot(slot).copy()
    else null
  }

  @Callback(doc = "function(slot:number):table -- Get the representation of the item stack stored in the specified slot.")
  def get(context: Context, args: Arguments): Array[AnyRef] = result(data.getStackInSlot(args.checkSlot(data, 0)))

  @Callback(doc = "function(slot:number):string -- Computes a hash value for the item stack in the specified slot.")
  def computeHash(context: Context, args: Arguments): Array[AnyRef] = {
    data.getStackInSlot(args.checkSlot(data, 0)) match {
      case stack: ItemStack =>
        val hash = Hashing.sha256().hashBytes(CompressedStreamTools.compress(stack))
        result(hash.toString)
      case _ => null
    }
  }

  @Callback(doc = "function(hash:string):number -- Get the index of an item stack with the specified hash. Returns a negative value if no such stack was found.")
  def indexOf(context: Context, args: Arguments): Array[AnyRef] = result(indexOf(args.checkString(0), 1))

  @Callback(doc = "function(address:string):number -- Copies the data stored in this database to another database with the specified address.")
  def clone(context: Context, args: Arguments): Array[AnyRef] = {
    node.network.node(args.checkString(0)) match {
      case component: Component => component.host match {
        case database: UpgradeDatabase =>
          val numberToCopy = math.min(data.getSizeInventory, database.data.getSizeInventory)
          for (slot <- 0 until numberToCopy) {
            database.data.setInventorySlotContents(slot, data.getStackInSlot(slot))
          }
          context.pause(0.25)
          result(numberToCopy)
        case _ => result(null, "not a database")
      }
      case _ => result(null, "no such component")
    }
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
