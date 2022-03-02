package li.cil.oc.integration.appeng

import appeng.tile.misc.TileInterface
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.internal.Database
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverBlockInterface extends DriverSidedTileEntity {
  def getTileEntityClass: Class[_] = classOf[TileInterface]

  def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileInterface])

  final class Environment(val tile: TileInterface) extends ManagedTileEntityEnvironment[TileInterface](tile, "me_interface") with NamedBlock with NetworkControl[TileInterface] {
    override def preferredName = "me_interface"

    override def priority = 5

    //noinspection ScalaUnusedSymbol
    @Callback(doc = "function([slot:number]):table -- Get the configuration of the interface.")
    def getInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val config = tileEntity.getInventoryByName("config")
      val slot = args.optSlot(config, 0, 0)
      val stack = config.getStackInSlot(slot)
      result(stack)
    }

    @Callback(doc = "function([slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface.")
    def setInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val config = tileEntity.getInventoryByName("config")
      val slot = if (args.isString(0)) 0 else args.optSlot(config, 0, 0)
      config.setInventorySlotContents(slot, getStack(args))
      context.pause(0.5)
      result(true)
    }

    private def getStack(args: Arguments) =
      if (args.count > 1) {
        val (address, entry, size) =
          if (args.isString(0)) (args.checkString(0), args.checkInteger(1), args.optInteger(2, 1))
          else (args.checkString(1), args.checkInteger(2), args.optInteger(3, 1))

        node.network.node(address) match {
          case component: Component => component.host match {
            case database: Database =>
              val dbStack = database.getStackInSlot(entry - 1)
              if (dbStack == null || size < 1) null
              else {
                dbStack.stackSize = math.min(size, dbStack.getMaxStackSize)
                dbStack
              }
            case _ => throw new IllegalArgumentException("not a database")
          }
          case _ => throw new IllegalArgumentException("no such component")
        }
      }
      else null

    //noinspection ScalaUnusedSymbol
    @Callback(doc = "function([slot:number]):table -- Get the given pattern in the interface.")
    def getInterfacePattern(context: Context, args: Arguments): Array[AnyRef] = {
      val inv = tileEntity.getInventoryByName("patterns")
      val slot = args.optSlot(inv, 0, 0)
      val stack = inv.getStackInSlot(slot)
      result(stack)
    }

    @Callback(doc = "function(slot:number, database:address, entry:number, size:number, index:number):boolean -- Set the pattern input at the given index.")
    def setInterfacePatternInput(context: Context, args: Arguments): Array[AnyRef] =
      setPatternSlot(context, args, "in")

    @Callback(doc = "function(slot:number, database:address, entry:number, size:number, index:number):boolean -- Set the pattern output at the given index.")
    def setInterfacePatternOutput(context: Context, args: Arguments): Array[AnyRef] =
      setPatternSlot(context, args, "out")

    @Callback(doc = "function(slot:number, index:number, database:address, entry:number):boolean -- Store pattern input at the given index to the database entry.")
    def storeInterfacePatternInput(context: Context, args: Arguments): Array[AnyRef] =
      storeInterfacePattern(context, args, "in")

    @Callback(doc = "function(slot:number, index:number, database:address, entry:number):boolean -- Store pattern output at the given index to the database entry.")
    def storeInterfacePatternOutput(context: Context, args: Arguments): Array[AnyRef] =
      storeInterfacePattern(context, args, "out")

    @Callback(doc = "function(slot:number, index:number):boolean -- Clear pattern input at the given index.")
    def clearInterfacePatternInput(context: Context, args: Arguments): Array[AnyRef] =
      clearInterfacePattern(context, args, "in")

    @Callback(doc = "function(slot:number, index:number):boolean -- Clear pattern output at the given index.")
    def clearInterfacePatternOutput(context: Context, args: Arguments): Array[AnyRef] =
      clearInterfacePattern(context, args, "out")


    private def storeInterfacePattern(context: Context, args: Arguments, tag: String): Array[AnyRef]  = {
      val inv = tileEntity.getInventoryByName("patterns")
      val pattern = inv.getStackInSlot(args.checkSlot(inv, 0))
      val encodedValue = pattern.getTagCompound
      if (encodedValue == null)
        throw new IllegalArgumentException("No pattern here!")
      val nbt = encodedValue.getTagList(tag, 10)
      val index = args.checkInteger(1) - 1
      if (index < 0)
        throw new IllegalArgumentException("Invalid index!")
      val stackNBT = nbt.getCompoundTagAt(index)
      val stack = ItemStack.loadItemStackFromNBT(stackNBT)
      DatabaseAccess.withDatabase(node, args.checkString(2), database => {
        val slot = args.optSlot(database.data, 3, 0)
        database.setStackInSlot(slot, stack)
        context.pause(0.1)
        result(true)
      })
    }

    private def clearInterfacePattern(context: Context, args: Arguments, tag: String): Array[AnyRef]  = {
      val inv = tileEntity.getInventoryByName("patterns")
      val slot = args.checkSlot(inv, 0)
      val pattern = inv.getStackInSlot(slot)
      val encodedValue = pattern.getTagCompound
      if (encodedValue == null)
        throw new IllegalArgumentException("No pattern here!")
      val nbt = encodedValue.getTagList(tag, 10)
      val index = args.checkInteger(1) - 1
      if (index < 0)
        throw new IllegalArgumentException("Invalid index!")
      nbt.removeTag(index)
      encodedValue.setTag(tag, nbt)
      pattern.setTagCompound(encodedValue)
      inv.setInventorySlotContents(slot, null)
      inv.setInventorySlotContents(slot, pattern)
      context.pause(0.1)
      result(true)
    }

    private def setPatternSlot(context: Context, args: Arguments, tag: String): Array[AnyRef] = {
      val inv = tileEntity.getInventoryByName("patterns")
      val slot = if (args.isString(0)) 0 else args.optSlot(inv, 0, 0)
      val stack = getStack(args)
      val index = args.checkInteger(4) - 1
      if (index < 0 || index > 511)
        throw new IllegalArgumentException("Invalid index!")
      val pattern = inv.getStackInSlot(slot)
      val encodedValue = pattern.getTagCompound
      if (encodedValue == null)
        throw new IllegalArgumentException("No pattern here!")
      val inTag = encodedValue.getTagList(tag, 10)
      while (inTag.tagCount() <= index)
        inTag.appendTag(new NBTTagCompound())
      if (stack != null) {
        val nbt = new NBTTagCompound()
        stack.writeToNBT(nbt)
        inTag.func_150304_a(index, nbt)
      }
      else
        inTag.removeTag(index)
      encodedValue.setTag(tag, inTag)
      pattern.setTagCompound(encodedValue)
      inv.setInventorySlotContents(slot, null)
      inv.setInventorySlotContents(slot, pattern)
      context.pause(0.1)
      result(true)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isBlockInterface(stack))
        classOf[Environment]
      else null
  }

}