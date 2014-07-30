package li.cil.oc.server.component.robot

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagList

import scala.util.control.Breaks._

class Inventory(val robot: tileentity.Robot) extends InventoryPlayer(null) {
  def selectedSlot = robot.selectedSlot

  def selectedItemStack = robot.getStackInSlot(selectedSlot)

  def firstInventorySlot = robot.actualSlot(0)

  def inventorySlots = (robot.selectedSlot until getSizeInventory) ++ (firstInventorySlot until robot.selectedSlot)

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

  override def decrementAnimations() {
    for (slot <- 0 until getSizeInventory) {
      Option(getStackInSlot(slot)) match {
        case Some(stack) => try stack.updateAnimation(robot.world, if (robot.isServer) robot.player() else null, slot, slot == 0) catch {
          case ignored: NullPointerException => // Client side item updates that need a player instance...
        }
        case _ =>
      }
    }
  }

  override def consumeInventoryItem(itemId: Int): Boolean = {
    for ((slot, stack) <- inventorySlots.map(slot => (slot, getStackInSlot(slot))) if stack != null && stack.itemID == itemId && stack.stackSize > 0) {
      stack.stackSize -= 1
      if (stack.stackSize <= 0) {
        setInventorySlotContents(slot, null)
      }
      return true
    }
    false
  }

  override def hasItem(itemId: Int) = (firstInventorySlot until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.itemID == itemId)

  override def addItemStackToInventory(stack: ItemStack) = {
    if (stack == null || stack.stackSize == 0) false
    else if (stack.isItemDamaged || (stack.stackSize == 1 && stack.getMaxStackSize == 1)) {
      val slot = getFirstEmptyStackAccepting(stack)
      if (slot >= firstInventorySlot) {
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
            if (slot >= firstInventorySlot) {
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
                  (!existing.getHasSubtypes || (existing.getItemDamage == stack.getItemDamage && ItemStack.areItemStackTagsEqual(existing, stack))) &&
                  (existing.stackSize < math.min(existing.getMaxStackSize, getInventoryStackLimit))
              }).getOrElse(getFirstEmptyStackAccepting(stack))
            if (slot >= firstInventorySlot) {
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

  override def decrStackSize(slot: Int, amount: Int) = robot.decrStackSize(slot, amount)

  override def getStackInSlotOnClosing(slot: Int) = robot.getStackInSlotOnClosing(slot)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = robot.setInventorySlotContents(slot, stack)

  override def getStrVsBlock(block: Block) = Option(getCurrentItem).fold(1f)(_.getStrVsBlock(block))

  override def writeToNBT(nbt: NBTTagList) = nbt

  override def readFromNBT(nbt: NBTTagList) {}

  override def getSizeInventory = 1 + robot.containerCount + robot.inventorySize

  override def getStackInSlot(slot: Int) = robot.getStackInSlot(slot)

  override def getInvName = robot.getInvName

  override def getInventoryStackLimit = robot.getInventoryStackLimit

  override def armorItemInSlot(slot: Int) = null

  override def getTotalArmorValue = 0

  override def damageArmor(damage: Float) {}

  override def dropAllItems() = robot.dropAllSlots()

  override def onInventoryChanged() = robot.onInventoryChanged()

  override def hasItemStack(stack: ItemStack) = (firstInventorySlot until getSizeInventory).map(getStackInSlot).filter(_ != null).exists(_.isItemEqual(stack))

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = robot.isItemValidForSlot(slot, stack)

  override def copyInventory(from: InventoryPlayer) {}
}
