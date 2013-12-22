package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.{TileEntity => MCTileEntity, TileEntityFurnace}
import scala.Some

class Generator(val owner: MCTileEntity) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("generator", Visibility.Neighbors).
    withConnector().
    create()

  var inventory: Option[ItemStack] = None

  var remainingTicks = 0

  // ----------------------------------------------------------------------- //

  @LuaCallback("insert")
  def insert(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else 64
    val player = context.player
    val stack = player.inventory.getStackInSlot(context.selectedSlot)
    if (stack == null) throw new IllegalArgumentException("selected slot is empty")
    if (!TileEntityFurnace.isItemFuel(stack)) return result(false, "selected slot does not contain fuel")
    inventory match {
      case Some(existingStack) =>
        if (!existingStack.isItemEqual(stack) ||
          !ItemStack.areItemStackTagsEqual(existingStack, stack)) {
          return result(false, "different fuel type already queued")
        }
        val space = existingStack.getMaxStackSize - existingStack.stackSize
        if (space <= 0) {
          return result(false, "queue is full")
        }
        val moveCount = math.min(stack.stackSize, math.min(space, count))
        existingStack.stackSize += moveCount
        stack.stackSize -= moveCount
      case _ =>
        inventory = Some(stack.splitStack(math.min(stack.stackSize, count)))
    }
    player.inventory.setInventorySlotContents(context.selectedSlot, stack)
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
  def remove(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else Int.MaxValue
    inventory match {
      case Some(stack) =>
        val removedStack = stack.splitStack(math.min(count, stack.stackSize))
        val success = context.player.inventory.addItemStackToInventory(removedStack)
        stack.stackSize += removedStack.stackSize
        if (success && stack.stackSize <= 0) {
          inventory = None
        }
        result(success)
      case _ => result(false)
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()
    if (remainingTicks <= 0 && inventory.isDefined) {
      val stack = inventory.get
      remainingTicks = TileEntityFurnace.getItemBurnTime(stack)
      updateClient()
      stack.stackSize -= 1
      if (stack.stackSize <= 0) {
        inventory = None
      }
    }
    if (remainingTicks > 0) {
      remainingTicks -= 1
      if (remainingTicks == 0 && inventory.isEmpty) {
        updateClient()
      }
      node.changeBuffer(Settings.get.ratioBuildCraft * Settings.get.generatorEfficiency)
    }
  }

  private def updateClient() = owner match {
    case robot: RobotContext => robot.saveUpgrade()
    case _ =>
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      inventory match {
        case Some(stack) =>
          val world = owner.getWorldObj
          val x = owner.xCoord
          val y = owner.yCoord
          val z = owner.zCoord
          val entity = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack.copy())
          entity.motionY = 0.04
          entity.delayBeforeCanPickup = 5
          world.spawnEntityInWorld(entity)
          inventory = None
        case _ =>
      }
    }
    remainingTicks = 0
  }

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
      case Some(stack) =>
        nbt.setNewCompoundTag("inventory", stack.writeToNBT)
      case _ =>
        nbt.removeTag("inventory")
    }
    if (remainingTicks > 0) {
      nbt.setInteger("remainingTicks", remainingTicks)
    }
    else {
      nbt.removeTag("remainingTicks")
    }
  }
}
