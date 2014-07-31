package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.Container
import li.cil.oc.api.machine.Robot
import li.cil.oc.api.network._
import li.cil.oc.common.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityFurnace

class UpgradeGenerator(val owner: Container with Robot) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("generator", Visibility.Neighbors).
    withConnector().
    create()

  val romGenerator = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/generator"), "generator"))

  var inventory: Option[ItemStack] = None

  var remainingTicks = 0

  def slot = (0 until owner.getSizeInventory).indexWhere(owner.getComponentInSlot(_) == this)

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function([count:number]):boolean -- Tries to insert fuel from the selected slot into the generator's queue.""")
  def insert(context: Context, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else 64
    val player = owner.player
    val stack = player.inventory.getStackInSlot(owner.selectedSlot)
    if (stack == null) return result(Unit, "selected slot is empty")
    if (!TileEntityFurnace.isItemFuel(stack)) {
      return result(Unit, "selected slot does not contain fuel")
    }
    inventory match {
      case Some(existingStack) =>
        if (!existingStack.isItemEqual(stack) ||
          !ItemStack.areItemStackTagsEqual(existingStack, stack)) {
          return result(Unit, "different fuel type already queued")
        }
        val space = existingStack.getMaxStackSize - existingStack.stackSize
        if (space <= 0) {
          return result(Unit, "queue is full")
        }
        val moveCount = math.min(stack.stackSize, math.min(space, count))
        existingStack.stackSize += moveCount
        stack.stackSize -= moveCount
      case _ =>
        inventory = Some(stack.splitStack(math.min(stack.stackSize, count)))
    }
    if (stack.stackSize > 0) player.inventory.setInventorySlotContents(owner.selectedSlot, stack)
    else player.inventory.setInventorySlotContents(owner.selectedSlot, null)
    result(true)
  }

  @Callback(doc = """function():number -- Get the size of the item stack in the generator's queue.""")
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    inventory match {
      case Some(stack) => result(stack.stackSize)
      case _ => result(0)
    }
  }

  @Callback(doc = """function([count:number]):boolean -- Tries to remove items from the generator's queue.""")
  def remove(context: Context, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else Int.MaxValue
    inventory match {
      case Some(stack) =>
        val removedStack = stack.splitStack(math.min(count, stack.stackSize))
        val success = owner.player.inventory.addItemStackToInventory(removedStack)
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
      node.changeBuffer(Settings.get.generatorEfficiency)
    }
  }

  private def updateClient() = owner.synchronizeSlot(slot)

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node.isNeighborOf(this.node)) {
      romGenerator.foreach(fs => node.connect(fs.node))
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      inventory match {
        case Some(stack) =>
          val world = owner.world
          val entity = new EntityItem(world, owner.xPosition, owner.yPosition, owner.zPosition, stack.copy())
          entity.motionY = 0.04
          entity.delayBeforeCanPickup = 5
          world.spawnEntityInWorld(entity)
          inventory = None
        case _ =>
      }
      remainingTicks = 0
      romGenerator.foreach(_.node.remove())
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romGenerator.foreach(_.load(nbt.getCompoundTag("romGenerator")))
    if (nbt.hasKey("inventory")) {
      inventory = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("inventory")))
    }
    remainingTicks = nbt.getInteger("remainingTicks")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romGenerator.foreach(fs => nbt.setNewCompoundTag("romGenerator", fs.save))
    inventory match {
      case Some(stack) => nbt.setNewCompoundTag("inventory", stack.writeToNBT)
      case _ =>
    }
    if (remainingTicks > 0) {
      nbt.setInteger("remainingTicks", remainingTicks)
    }
  }
}
