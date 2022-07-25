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
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.entity.item.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.common.ForgeHooks

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
    val stack = host.mainInventory.getItem(host.selectedSlot)
    if (stack.isEmpty) return result(Unit, "selected slot is empty")
    if (ForgeHooks.getBurnTime(stack, null) <= 0) {
      return result(Unit, "selected slot does not contain fuel")
    }
    val container: ItemStack = stack.getContainerItem()
    val inQueue: ItemStack = inventory match {
      case SomeStack(q) if q != null && q.getCount > 0 =>
        if (!q.sameItem(stack) || !ItemStack.tagMatches(q, stack)) {
          return result(Unit, "different fuel type already queued")
        }
        q
      case _ => ItemStack.EMPTY
    }
    val space = if (inQueue.isEmpty) stack.getMaxStackSize else inQueue.getMaxStackSize - inQueue.getCount
    if (space == 0) {
      return result(Unit, "queue is full")
    }
    val previousSelectedFuel: ItemStack = stack.copy
    val insertLimit: Int = math.min(stack.getCount, math.min(space, count))
    val fuelToInsert: ItemStack = stack.split(insertLimit)

    // remove the fuel from the inventory
    if (stack.getCount == 0) {
      host.mainInventory.setItem(host.selectedSlot, ItemStack.EMPTY)
    } else {
      host.mainInventory.setItem(host.selectedSlot, stack)
    }

    // add empty containers to inventory
    if (!container.isEmpty) {
      container.grow(fuelToInsert.getCount - 1)
      if (!host.player.inventory.add(container)) {
        // no containers could be placed in inventory, give back the fuel
        host.mainInventory.setItem(host.selectedSlot, previousSelectedFuel)
        return result(false, "no space in inventory for fuel containers")
      } else if (container.getCount > 0) {
        // not all the containers could be inserted in the inventory
        host.player.spawnAtLocation(container.copy, -0.25f)
      }
    }

    // could be zero
    fuelToInsert.grow(inQueue.getCount)
    inventory = StackOption(fuelToInsert)

    result(true, insertLimit)
  }

  @Callback(doc = """function():number -- Get the size of the item stack in the generator's queue.""")
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    inventory match {
      case SomeStack(stack) => result(stack.getCount, stack.getItem.getName(stack))
      case _ => result(0)
    }
  }

  @Callback(doc = """function([count:number]):boolean -- Tries to remove items from the generator's queue.""")
  def remove(context: Context, args: Arguments): Array[AnyRef] = {
    val count = args.optInteger(0, Int.MaxValue)
    if (count <= 0) {
      return result(true) // it is allowed to remove zero
    }
    val inQueue: ItemStack = inventory match {
      case SomeStack(q) if !q.isEmpty && q.getCount > 0 => q
      case _ => ItemStack.EMPTY
    }
    if (inQueue.isEmpty) {
      return result(false, "queue is empty")
    }
    val previousSelectedItem: ItemStack = host.mainInventory.getItem(host.selectedSlot).copy
    val emptyContainer: ItemStack = inQueue.getContainerItem match {
      case requiredContainer if !requiredContainer.isEmpty && requiredContainer.getCount > 0 => previousSelectedItem match {
        case slotItem: ItemStack if !slotItem.isEmpty &&
          slotItem.getItem == requiredContainer.getItem &&
          ItemStack.tagMatches(slotItem, requiredContainer) => slotItem.copy
        case _ => return result(false, "removing this fuel requires the appropriate container in the selected slot")
      }
      case _ => ItemStack.EMPTY // nothing to do, nothing required
    }

    val removeLimit: Int = math.min(inQueue.getCount, if (emptyContainer.isEmpty) count else emptyContainer.getCount)

    // backup in case of failure
    val previousQueue = inQueue.copy
    val forUser = inQueue.split(removeLimit)
    if (!emptyContainer.isEmpty) {
      emptyContainer.split(removeLimit)
      if (emptyContainer.isEmpty) {
        host.mainInventory.setItem(host.selectedSlot, ItemStack.EMPTY)
      } else {
        host.mainInventory.removeItem(host.selectedSlot, removeLimit)
      }
    }
    // add splits the input stack by reference
    if (!host.player.inventory.add(forUser)) {
      // returns false if NO items were inserted
      host.mainInventory.setItem(host.selectedSlot, previousSelectedItem)
      inventory = StackOption(previousQueue)
      result (false, "no inventory space available for fuel")
    } else {
      val actualRemoval: Int = removeLimit - forUser.getCount
      previousQueue.shrink(actualRemoval) // reduce it by how much was given to the user
      inventory = StackOption(previousQueue)
      result(true, actualRemoval)
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()
    if (remainingTicks <= 0 && inventory.isDefined) {
      val stack = inventory.get
      remainingTicks = ForgeHooks.getBurnTime(stack, null)
      if (remainingTicks > 0) {
        updateClient()
        stack.shrink(1)
        if (stack.getCount <= 0) {
            // do not put container in inventory (we left the container when fuel was inserted)
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

  private def updateClient(): Unit = host match {
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
          val entity = new ItemEntity(world, host.xPosition, host.yPosition, host.zPosition, stack.copy())
          entity.setDeltaMovement(entity.getDeltaMovement.add(0, 0.04, 0))
          entity.setPickUpDelay(5)
          world.addFreshEntity(entity)
          inventory = EmptyStack
        case _ =>
      }
      remainingTicks = 0
    }
  }

  private final val InventoryTag = "inventory"
  private final val RemainingTicksTag = "remainingTicks"

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
      inventory = StackOption(ItemStack.of(nbt.getCompound("inventory")))
    if (nbt.contains(InventoryTag)) {
      inventory = StackOption(ItemStack.of(nbt.getCompound(InventoryTag)))
    }
    remainingTicks = nbt.getInt(RemainingTicksTag)
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)
    inventory match {
      case SomeStack(stack) => nbt.setNewCompoundTag(InventoryTag, stack.save)
      case _ =>
    }
    if (remainingTicks > 0) {
      nbt.putInt(RemainingTicksTag, remainingTicks)
    }
  }
}
