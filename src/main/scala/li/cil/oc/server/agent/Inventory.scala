package li.cil.oc.server.agent

import li.cil.oc.api.internal
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList
import li.cil.oc.util.ExtendedInventory._

class Inventory(val agent: internal.Agent) extends InventoryPlayer(null) {
  private def selectedItemStack = agent.mainInventory.getStackInSlot(agent.selectedSlot)

  private def inventorySlots = (agent.selectedSlot until getSizeInventory) ++ (0 until agent.selectedSlot)

  override def getCurrentItem = agent.equipmentInventory.getStackInSlot(0)

  override def getFirstEmptyStack = {
    if (selectedItemStack == null) agent.selectedSlot
    else inventorySlots.find(getStackInSlot(_) == null).getOrElse(-1)
  }

  override def func_146030_a(p_146030_1_ : Item, p_146030_2_ : Int, p_146030_3_ : Boolean, p_146030_4_ : Boolean) = setCurrentItem(p_146030_1_, p_146030_2_, p_146030_3_, p_146030_4_)

  def setCurrentItem(item: Item, itemDamage: Int, checkDamage: Boolean, create: Boolean) {}

  override def changeCurrentItem(direction: Int) {}

  override def clearInventory(item: Item, itemDamage: Int) = 0

  override def func_70439_a(item: Item, itemDamage: Int) {}

  override def decrementAnimations() {
    for (slot <- 0 until getSizeInventory) {
      Option(getStackInSlot(slot)) match {
        case Some(stack) => try stack.updateAnimation(agent.world, if (!agent.world.isRemote) agent.player else null, slot, slot == 0) catch {
          case ignored: NullPointerException => // Client side item updates that need a player instance...
        }
        case _ =>
      }
    }
  }

  override def consumeInventoryItem(item: Item): Boolean = {
    for ((slot, stack) <- inventorySlots.map(slot => (slot, getStackInSlot(slot))) if stack != null && stack.getItem == item && stack.stackSize > 0) {
      stack.stackSize -= 1
      if (stack.stackSize <= 0) {
        setInventorySlotContents(slot, null)
      }
      return true
    }
    false
  }

  override def addItemStackToInventory(stack: ItemStack) = {
    super.addItemStackToInventory(stack)
    val slots = this.indices.drop(agent.selectedSlot) ++ this.indices.take(agent.selectedSlot)
    InventoryUtils.insertIntoInventory(stack, this, slots = Option(slots))
  }

  override def func_146025_b(block: Block) = canHarvestBlock(block)

  def canHarvestBlock(block: Block): Boolean = {
    block.getMaterial.isToolNotRequired || (getCurrentItem != null && getCurrentItem.func_150998_b(block))
  }

  override def func_146023_a(block: Block) = getStrVsBlock(block)

  def getStrVsBlock(block: Block) = Option(getCurrentItem).fold(1f)(_.func_150997_a(block))

  override def writeToNBT(nbt: NBTTagList) = nbt

  override def readFromNBT(nbt: NBTTagList) {}

  override def armorItemInSlot(slot: Int) = null

  override def getTotalArmorValue = 0

  override def damageArmor(damage: Float) {}

  override def dropAllItems() = {}

  override def hasItem(item: Item) = (0 until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.getItem == item)

  override def hasItemStack(stack: ItemStack) = (0 until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.isItemEqual(stack))

  override def copyInventory(from: InventoryPlayer) {}

  // IInventory

  override def getSizeInventory = agent.mainInventory.getSizeInventory

  override def getStackInSlot(slot: Int) =
    if (slot < 0) agent.equipmentInventory.getStackInSlot(~slot)
    else agent.mainInventory.getStackInSlot(slot)

  override def decrStackSize(slot: Int, amount: Int) = {
    super.decrStackSize(slot, amount)
    if (slot < 0) agent.equipmentInventory.decrStackSize(~slot, amount)
    else agent.mainInventory.decrStackSize(slot, amount)
  }

  override def getStackInSlotOnClosing(slot: Int) =
    if (slot < 0) agent.equipmentInventory.getStackInSlotOnClosing(~slot)
    else agent.mainInventory.getStackInSlotOnClosing(slot)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    super.setInventorySlotContents(slot, stack)
    if (slot < 0) agent.equipmentInventory.setInventorySlotContents(~slot, stack)
    else agent.mainInventory.setInventorySlotContents(slot, stack)
  }

  override def getInventoryName = agent.mainInventory.getInventoryName

  override def getInventoryStackLimit = agent.mainInventory.getInventoryStackLimit

  override def markDirty() = agent.mainInventory.markDirty()

  override def isUseableByPlayer(player: EntityPlayer) = agent.mainInventory.isUseableByPlayer(player)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot < 0) agent.equipmentInventory.isItemValidForSlot(~slot, stack)
    else agent.mainInventory.isItemValidForSlot(slot, stack)
}
