package li.cil.oc.common.tileentity

import java.util.concurrent.atomic.AtomicBoolean
import li.cil.oc.server.computer.IComputerEnvironment
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer
import java.lang.Boolean
import net.minecraft.nbt.NBTTagList

class TileEntityComputer(isClient: Boolean) extends TileEntity with IComputerEnvironment with IInventory {
  var inv = new Array[ItemStack](9)

  def this() = this(false)
  MinecraftForge.EVENT_BUS.register(this)

  private val hasChanged = new AtomicBoolean()

  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  private val computer =
    if (isClient) new li.cil.oc.client.computer.Computer(this)
    else new li.cil.oc.server.computer.Computer(this)

  def turnOn() = computer.start()

  def turnOff() = computer.stop()

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    computer.readFromNBT(nbt)

    var tagList = nbt.getTagList("Inventory");
    for (i <- 0 until tagList.tagCount()) {
      var tag = tagList.tagAt(i).asInstanceOf[NBTTagCompound];
      var slot = tag.getByte("Slot");
      if (slot >= 0 && slot < inv.length) {
        inv(slot) = ItemStack.loadItemStackFromNBT(tag);
      }
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    println("SAVING")
    super.writeToNBT(nbt)
    computer.writeToNBT(nbt)

    var itemList = new NBTTagList();
    for (i <- 0 until inv.length) {
      var stack = inv(i);
      if (stack != null) {
        var tag = new NBTTagCompound();
        tag.setByte("Slot", i.asInstanceOf[Byte]);
        stack.writeToNBT(tag);
        itemList.appendTag(tag);
      }
    }
    nbt.setTag("Inventory", itemList);
  }

  override def updateEntity() = {
    computer.update()
    if (hasChanged.get())
      worldObj.updateTileEntityChunkAndDoNothing(
        this.xCoord, this.yCoord, this.zCoord, this)
  }
  // ----------------------------------------------------------------------- //
  // Inventory
  // ----------------------------------------------------------------------- //

  /**
   * Returns the number of slots in the inventory.
   */
  override def getSizeInventory() = inv.length

  /**
   * Returns the stack in slot i
   */
  override def getStackInSlot(i: Int) = inv(i)

  /**
   * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
   * new stack.
   */
  override def decrStackSize(slot: Int, amt: Int): ItemStack = {
    var stack = getStackInSlot(slot);
    if (stack != null) {
      if (stack.stackSize <= amt) {
        setInventorySlotContents(slot, null);
      } else {
        stack = stack.splitStack(amt);
        if (stack.stackSize == 0) {
          setInventorySlotContents(slot, null);
        }
      }
    }
    return stack;
  }

  /**
   * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
   * like when you close a workbench GUI.
   */
  override def getStackInSlotOnClosing(slot: Int): ItemStack = {
    var stack = getStackInSlot(slot);
    if (stack != null) {
      setInventorySlotContents(slot, null);
    }

    return stack;
  }

  /**
   * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
   */
  override def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    inv(slot) = stack;
    if (stack != null && (stack.stackSize > getInventoryStackLimit())) {
      stack.stackSize = getInventoryStackLimit();
    }
  }

  /**
   * Returns the name of the inventory.
   */
  override def getInvName() = "oc.tileentitycomputer"

  /**
   * If this returns false, the inventory name will be used as an unlocalized name, and translated into the player's
   * language. Otherwise it will be used directly.
   */
  override def isInvNameLocalized() = false

  /**
   * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
   * this more of a set than a get?*
   */
  override def getInventoryStackLimit() = 64

  /**
   * Called when an the contents of an Inventory change, usually
   */
  override def onInventoryChanged() {
    //ka ^^
  }

  /**
   * Do not make give this method the name canInteractWith because it clashes with Container
   */
  override def isUseableByPlayer(entityplayer: EntityPlayer) =
    (worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      entityplayer.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64)

  override def openChest() {

  }

  override def closeChest() {

  }

  /**
   * Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot.
   */
  override def isItemValidForSlot(i: Int, itemstack: ItemStack) = false
  
 

  // ----------------------------------------------------------------------- //
  // Event Bus
  // ----------------------------------------------------------------------- //

  @ForgeSubscribe
  def onChunkUnload(e: ChunkEvent.Unload) = {
    println("CHUNK UNLOADING")
    MinecraftForge.EVENT_BUS.unregister(this)
    computer.stop()
  }

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) = {
    println("WORLD UNLOADING")
    MinecraftForge.EVENT_BUS.unregister(this)
    computer.stop()
  }

  // ----------------------------------------------------------------------- //
  // IComputerEnvironment
  // ----------------------------------------------------------------------- //

  def world = worldObj

  def markAsChanged() = hasChanged.set(true)
}