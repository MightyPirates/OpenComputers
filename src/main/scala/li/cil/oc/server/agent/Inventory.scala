package li.cil.oc.server.agent

import li.cil.oc.api.internal
import li.cil.oc.util.ExtendedInventory._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.block.state.IBlockState

class Inventory(playerEntity: EntityPlayer, val agent: internal.Agent) extends InventoryPlayer(playerEntity) {

  private def selectedItemStack = agent.mainInventory.getStackInSlot(agent.selectedSlot)

  private def inventorySlots = (agent.selectedSlot until getSizeInventory) ++ (0 until agent.selectedSlot)

  override def getCurrentItem = agent.equipmentInventory.getStackInSlot(0)

  override def getFirstEmptyStack = {
    if (selectedItemStack == null) agent.selectedSlot
    else inventorySlots.find(getStackInSlot(_) == null).getOrElse(-1)
  }

  override def changeCurrentItem(direction: Int) {}

  override def clearMatchingItems(item: Item, damage: Int, count: Int, tag: NBTTagCompound): Int = 0

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

//  override def consumeInventoryItem(item: Item): Boolean = {
//    for ((slot, stack) <- inventorySlots.map(slot => (slot, getStackInSlot(slot))) if stack != null && stack.getItem == item && stack.stackSize > 0) {
//      stack.stackSize -= 1
//      if (stack.stackSize <= 0) {
//        setInventorySlotContents(slot, null)
//      }
//      return true
//    }
//    false
//  }

  override def addItemStackToInventory(stack: ItemStack) = {
    super.addItemStackToInventory(stack)
    val slots = this.indices.drop(agent.selectedSlot) ++ this.indices.take(agent.selectedSlot)
    InventoryUtils.insertIntoInventory(stack, InventoryUtils.asItemHandler(this), slots = Option(slots))
  }

  override def canHarvestBlock(state: IBlockState): Boolean = state.getMaterial.isToolNotRequired || (getCurrentItem != null && getCurrentItem.canHarvestBlock(state))

  override def getStrVsBlock(state: IBlockState): Float = Option(getCurrentItem).fold(1f)(_.getStrVsBlock(state))

  override def writeToNBT(nbt: NBTTagList) = nbt

  override def readFromNBT(nbt: NBTTagList) {}

  override def armorItemInSlot(slot: Int) = null

  override def damageArmor(damage: Float) {}

  override def dropAllItems() = {}

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

  override def removeStackFromSlot(slot: Int) = {
    super.removeStackFromSlot(slot)
    if (slot < 0) agent.equipmentInventory.removeStackFromSlot(~slot)
    else agent.mainInventory.removeStackFromSlot(slot)
  }

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    super.setInventorySlotContents(slot, stack)
    if (slot < 0) agent.equipmentInventory.setInventorySlotContents(~slot, stack)
    else agent.mainInventory.setInventorySlotContents(slot, stack)
  }

  override def getName = agent.mainInventory.getName

  override def getInventoryStackLimit = agent.mainInventory.getInventoryStackLimit

  override def markDirty() = agent.mainInventory.markDirty()

  override def isUseableByPlayer(player: EntityPlayer) = agent.mainInventory.isUseableByPlayer(player)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot < 0) agent.equipmentInventory.isItemValidForSlot(~slot, stack)
    else agent.mainInventory.isItemValidForSlot(slot, stack)
}
