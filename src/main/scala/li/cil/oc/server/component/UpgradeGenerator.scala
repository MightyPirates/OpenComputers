package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityFurnace

import scala.collection.convert.WrapAsJava._

class UpgradeGenerator(val host: EnvironmentHost with internal.Agent) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("generator", Visibility.Neighbors).
    withConnector().
    create()

  var inventory: StackOption = EmptyStack

  var remainingTicks = 0

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Generator",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Portagen 2.0 (Rev. 3)",
    DeviceAttribute.Capacity -> "1"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function([count:number]):boolean -- Tries to insert fuel from the selected slot into the generator's queue.""")
  def insert(context: Context, args: Arguments): Array[AnyRef] = {
    val count = args.optInteger(0, 64)
    val stack = host.mainInventory.getStackInSlot(host.selectedSlot)
    if (stack.isEmpty) return result(Unit, "selected slot is empty")
    if (!TileEntityFurnace.isItemFuel(stack)) {
      return result(Unit, "selected slot does not contain fuel")
    }
    inventory match {
      case SomeStack(existingStack) =>
        if (!existingStack.isItemEqual(stack) ||
          !ItemStack.areItemStackTagsEqual(existingStack, stack)) {
          return result(Unit, "different fuel type already queued")
        }
        val space = existingStack.getMaxStackSize - existingStack.getCount
        if (space <= 0) {
          return result(Unit, "queue is full")
        }
        val moveCount = math.min(stack.getCount, math.min(space, count))
        existingStack.grow(moveCount)
        stack.shrink(moveCount)
      case _ =>
        inventory = StackOption(stack.splitStack(math.min(stack.getCount, count)))
    }
    if (stack.getCount > 0) host.mainInventory.setInventorySlotContents(host.selectedSlot, stack)
    else host.mainInventory.setInventorySlotContents(host.selectedSlot, ItemStack.EMPTY)
    result(true)
  }

  @Callback(doc = """function():number -- Get the size of the item stack in the generator's queue.""")
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    inventory match {
      case SomeStack(stack) => result(stack.getCount)
      case _ => result(0)
    }
  }

  @Callback(doc = """function([count:number]):boolean -- Tries to remove items from the generator's queue.""")
  def remove(context: Context, args: Arguments): Array[AnyRef] = {
    val count = args.optInteger(0, Int.MaxValue)
    inventory match {
      case SomeStack(stack) =>
        val removedStack = stack.splitStack(math.min(count, stack.getCount))
        val success = host.player.inventory.addItemStackToInventory(removedStack)
        stack.grow(removedStack.getCount)
        if (success && stack.getCount <= 0) {
          inventory = EmptyStack
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
      if (remainingTicks > 0) {
        // If not we probably have a container item now (e.g. bucket after lava bucket).
        updateClient()
        stack.shrink(1)
        if (stack.getCount <= 0) {
          if (stack.getItem.hasContainerItem(stack))
            inventory = StackOption(stack.getItem.getContainerItem(stack))
          else
            inventory = EmptyStack
        }
      }
    }
    if (remainingTicks > 0) {
      remainingTicks -= 1
      if (remainingTicks == 0 && inventory.isEmpty) {
        updateClient()
      }
      node.changeBuffer(Settings.get.generatorEfficiency)
    }
  }

  private def updateClient() = host match {
    case robot: internal.Robot => robot.synchronizeSlot(robot.componentSlot(node.address))
    case _ =>
  }

  // ----------------------------------------------------------------------- //

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      inventory match {
        case SomeStack(stack) =>
          val world = host.world
          val entity = new EntityItem(world, host.xPosition, host.yPosition, host.zPosition, stack.copy())
          entity.motionY = 0.04
          entity.setPickupDelay(5)
          world.spawnEntity(entity)
          inventory = EmptyStack
        case _ =>
      }
      remainingTicks = 0
    }
  }

  private final val InventoryTag = "inventory"
  private final val RemainingTicksTag = "remainingTicks"

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
      inventory = StackOption(new ItemStack(nbt.getCompoundTag("inventory")))
    if (nbt.hasKey(InventoryTag)) {
      inventory = StackOption(new ItemStack(nbt.getCompoundTag(InventoryTag)))
    }
    remainingTicks = nbt.getInteger(RemainingTicksTag)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    inventory match {
      case SomeStack(stack) => nbt.setNewCompoundTag(InventoryTag, stack.writeToNBT)
      case _ =>
    }
    if (remainingTicks > 0) {
      nbt.setInteger(RemainingTicksTag, remainingTicks)
    }
  }
}
