package li.cil.oc.util

import net.minecraft.block.Block
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagList
import scala.util.control.Breaks._

class InventoryRobot(player: RobotPlayer) extends InventoryPlayer(player) {
  val robot = player.robot

  def selectedSlot = robot.actualSlot(robot.selectedSlot)

  def selectedItemStack = robot.getStackInSlot(selectedSlot)

  def inventorySlots = (robot.actualSlot(robot.selectedSlot) until getSizeInventory) ++ (0 until robot.actualSlot(robot.selectedSlot))

  override def getCurrentItem = getStackInSlot(0)

  override def getFirstEmptyStack = {
    if (selectedItemStack == null) selectedSlot
    else inventorySlots.find(getStackInSlot(_) == null).getOrElse(-1)
  }

  def getFirstEmptyStackAccepting(stack: ItemStack) = {
    if (selectedItemStack == null && isItemValidForSlot(selectedSlot, stack)) selectedSlot
    else inventorySlots.find(slot => getStackInSlot(slot) == null && isItemValidForSlot(slot, stack)).getOrElse(-1)
  }

  override def setCurrentItem(itemId: Int, itemDamage: Int, checkDamage: Boolean, create: Boolean) {}

  override def changeCurrentItem(direction: Int) {}

  override def clearInventory(itemId: Int, itemDamage: Int) = 0

  override def func_70439_a(item: Item, itemDamage: Int) {}

  override def decrementAnimations() {}

  override def consumeInventoryItem(itemId: Int) = false

  override def hasItem(itemId: Int) = (0 until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.itemID == itemId)

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
              if (selectedItemStack == null) selectedSlot
              else inventorySlots.find(slot => {
                val existing = getStackInSlot(slot)
                existing != null && existing.isItemEqual(stack) &&
                  (!existing.getHasSubtypes || existing.getItemDamage == stack.getItemDamage) &&
                  (existing.stackSize < (existing.getMaxStackSize min getInventoryStackLimit))
              }).getOrElse(getFirstEmptyStackAccepting(stack))
            if (slot >= 0) {
              if (getStackInSlot(slot) == null) {
                val amount = stack.stackSize min (getInventoryStackLimit min stack.getMaxStackSize)
                setInventorySlotContents(slot, stack.splitStack(amount))
              }
              else {
                val existing = getStackInSlot(slot)
                val space = (getInventoryStackLimit min existing.getMaxStackSize) - existing.stackSize
                val amount = stack.stackSize min space
                existing.stackSize += amount
                stack.stackSize -= amount
              }
            }
            else break()
          }
        }
      }
      if (stack.stackSize < originalSize) {
        // This emulates behavior of the player's inventory, where surplus
        // items from a collected stack vanish (yes, they do, yes, it sucks).
        stack.stackSize = 0
        true
      }
      else false
    }
  }

  override def decrStackSize(slot: Int, amount: Int) = robot.decrStackSize(slot, amount)

  override def getStackInSlotOnClosing(slot: Int) = robot.getStackInSlotOnClosing(slot)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = robot.setInventorySlotContents(slot, stack)

  override def getStrVsBlock(block: Block) = Option(getCurrentItem).fold(1f)(_.getStrVsBlock(block))

  override def writeToNBT(nbt: NBTTagList) = nbt

  override def readFromNBT(nbt: NBTTagList) {}

  override def getSizeInventory = robot.getSizeInventory

  override def getStackInSlot(slot: Int) = robot.getStackInSlot(slot)

  override def getInvName = robot.getInvName

  override def getInventoryStackLimit = robot.getInventoryStackLimit

  override def armorItemInSlot(slot: Int) = null

  override def getTotalArmorValue = 0

  override def damageArmor(damage: Float) {}

  override def dropAllItems() = robot.dropAllSlots()

  override def onInventoryChanged() = robot.onInventoryChanged()

  override def hasItemStack(stack: ItemStack) = (0 until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.isItemEqual(stack))

  override def isItemValidForSlot(slot: Int, item: ItemStack) = robot.isItemValidForSlot(slot, item)

  override def copyInventory(from: InventoryPlayer) {}
}
