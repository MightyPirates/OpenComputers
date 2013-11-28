package li.cil.oc.server.component

import li.cil.oc.api.network.{Context, Arguments, LuaCallback, Visibility}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityFurnace

class Generator extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("generator", Visibility.Neighbors).
    withConnector().
    create()

  var inventory: Option[ItemStack] = None

  var remainingTicks = 0

  // ----------------------------------------------------------------------- //

  @LuaCallback("insert")
  def insert(context: Context, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else 64
    val stack = context.getStackInSelectedSlot
    if (stack == null) throw new IllegalArgumentException("selected slot is empty")
    if (!TileEntityFurnace.isItemFuel(stack)) return result(false, "selected slot does not contain fuel")
    inventory match {
      case Some(existingStack) =>
        if (!ItemStack.areItemStacksEqual(existingStack, stack) ||
          !ItemStack.areItemStackTagsEqual(existingStack, stack)) {
          return result(false, "different fuel type already queued")
        }
        val space = existingStack.getMaxStackSize - existingStack.stackSize
        if (space <= 0) {
          return result(false, "queue is full")
        }
        val moveCount = stack.stackSize min space min count
        existingStack.stackSize += moveCount
        stack.stackSize -= moveCount
      case _ =>
        inventory = Some(stack.splitStack(stack.getMaxStackSize min count))
    }
    context.setStackInSelectedSlot(stack)
    result(true)
  }

  @LuaCallback("count")
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    inventory match {
      case Some(stack) => result(stack.stackSize)
      case _ => result(0)
    }
  }

  @LuaCallback("remove")
  def remove(context: Context, args: Arguments): Array[AnyRef] = {
    null
  }

  // ----------------------------------------------------------------------- //

  override def update() {
    super.update()
    if (remainingTicks <= 0 && inventory.isDefined) {
      val stack = inventory.get
      remainingTicks = TileEntityFurnace.getItemBurnTime(stack)
      stack.stackSize -= 1
      if (stack.stackSize <= 0) {
        inventory = None
      }
    }
    if (remainingTicks > 0) {
      remainingTicks -= 1
      node.changeBuffer(Settings.get.ratioBuildCraft * Settings.get.generatorEfficiency)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("inventory")) {
      inventory = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("inventory")))
    }
    remainingTicks = nbt.getInteger("remainingTicks")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    inventory match {
      case Some(stack) => nbt.setNewCompoundTag("inventory", stack.writeToNBT)
      case _ => nbt.removeTag("inventory")
    }
    nbt.setInteger("remainingTicks", remainingTicks)
  }
}
