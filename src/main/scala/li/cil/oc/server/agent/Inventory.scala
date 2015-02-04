package li.cil.oc.server.agent

import li.cil.oc.api.internal
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList

import scala.util.control.Breaks._

class Inventory(val agent: internal.Agent) extends InventoryPlayer(null) {
  def selectedItemStack = agent.mainInventory.getStackInSlot(agent.selectedSlot)

  def inventorySlots = (agent.selectedSlot until getSizeInventory) ++ (0 until agent.selectedSlot)

  override def getCurrentItem = agent.equipmentInventory.getStackInSlot(0)

  override def getFirstEmptyStack = {
    if (selectedItemStack == null) agent.selectedSlot
    else inventorySlots.find(getStackInSlot(_) == null).getOrElse(-1)
  }

  def getFirstEmptyStackAccepting(stack: ItemStack) = {
    if (selectedItemStack == null && isItemValidForSlot(agent.selectedSlot, stack)) agent.selectedSlot
    else inventorySlots.find(slot => getStackInSlot(slot) == null && isItemValidForSlot(slot, stack)).getOrElse(-1)
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
    if (stack == null || stack.stackSize == 0) false
    else if (stack.isItemDamaged || (stack.stackSize == 1 && stack.getMaxStackSize == 1)) {
      val slot = getFirstEmptyStackAccepting(stack)
      if (slot >= 0) {
        setInventorySlotContents(slot, stack.splitStack(1))
        true
      }
      else false
    }
    else {
      val originalSize = stack.stackSize
      breakable {
        while (stack.stackSize > 0) {
          if (stack.getMaxStackSize == 1) {
            val slot = getFirstEmptyStackAccepting(stack)
            if (slot >= 0) {
              setInventorySlotContents(slot, stack.splitStack(1))
            }
            else break()
          }
          else {
            val slot =
              if (selectedItemStack == null) agent.selectedSlot
              else inventorySlots.find(slot => {
                val existing = getStackInSlot(slot)
                existing != null && existing.isItemEqual(stack) &&
                  (!existing.getHasSubtypes || (existing.getItemDamage == stack.getItemDamage && ItemStack.areItemStackTagsEqual(existing, stack))) &&
                  (existing.stackSize < math.min(existing.getMaxStackSize, getInventoryStackLimit))
              }).getOrElse(getFirstEmptyStackAccepting(stack))
            if (slot >= 0) {
              if (getStackInSlot(slot) == null) {
                val amount = math.min(stack.stackSize, math.min(getInventoryStackLimit, stack.getMaxStackSize))
                setInventorySlotContents(slot, stack.splitStack(amount))
              }
              else {
                val existing = getStackInSlot(slot)
                val space = math.min(getInventoryStackLimit, existing.getMaxStackSize) - existing.stackSize
                val amount = math.min(stack.stackSize, space)
                existing.stackSize += amount
                stack.stackSize -= amount
              }
            }
            else break()
          }
        }
      }
      stack.stackSize < originalSize
    }
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

  override def dropAllItems() = {} // TODO 1.5 agent.dropAllSlots()

  override def hasItem(item: Item) = (0 until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.getItem == item)

  override def hasItemStack(stack: ItemStack) = (0 until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.isItemEqual(stack))

  override def copyInventory(from: InventoryPlayer) {}

  // IInventory

  override def getSizeInventory = agent.mainInventory.getSizeInventory

  override def getStackInSlot(slot: Int) = agent.mainInventory.getStackInSlot(slot)

  override def decrStackSize(slot: Int, amount: Int) = agent.mainInventory.decrStackSize(slot, amount)

  override def getStackInSlotOnClosing(slot: Int) = agent.mainInventory.getStackInSlotOnClosing(slot)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = agent.mainInventory.setInventorySlotContents(slot, stack)

  override def getInventoryName = agent.mainInventory.getInventoryName

  override def getInventoryStackLimit = agent.mainInventory.getInventoryStackLimit

  override def markDirty() = agent.mainInventory.markDirty()

  override def isUseableByPlayer(player: EntityPlayer) = agent.mainInventory.isUseableByPlayer(player)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = agent.mainInventory.isItemValidForSlot(slot, stack)
}
