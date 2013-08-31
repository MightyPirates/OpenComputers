package li.cil.oc.container

import net.minecraft.inventory.Container
import net.minecraft.entity.player.InventoryPlayer
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.inventory.Slot
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class ContainerComputer(inventory: InventoryPlayer, tile: TileEntityComputer) extends Container {
  val tileEntity = tile;
  for (i <- 0 until 3) {
    for (j <- 0 until 3) {
      addSlotToContainer(new Slot(tile, j + i * 3, 62 + j * 18, 17 + i * 18){
        override def isItemValid(item:ItemStack)= false
      });
    }
  }
  bindPlayerInventory(inventory)

  def bindPlayerInventory(inventoryPlayer: InventoryPlayer) = {
    for (i <- 0 until 3) {
      for (j <- 0 until 9) {
        System.out.println("creating"+(j + i * 9 + 9))
        addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9,
          8 + j * 18, 84 + i * 18));
      }
    }

    for (i <- 0 until 9) {
      System.out.println("creating"+i)
      addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
    }
  }
  override def canInteractWith(player: EntityPlayer) = tileEntity.isUseableByPlayer(player);
  
  override def mergeItemStack(x$1:ItemStack, x$2:Int, x$3:Int, x$4:Boolean):Boolean={
    System.out.println(" merge")
    return super.mergeItemStack(x$1, x$2, x$3, x$4)
  }
  override def transferStackInSlot(player: EntityPlayer, slot: Int): ItemStack = {
    var stack: ItemStack = null
    var slotObject = inventorySlots.get(slot).asInstanceOf[Slot];

    //null checks and checks if the item can be stacked (maxStackSize > 1)
    if (slotObject != null && slotObject.getHasStack()) {
      var stackInSlot = slotObject.getStack();
      stack = stackInSlot.copy();

      //merges the item into player inventory since its in the tileEntity
      if (slot < 9) {
        if (!this.mergeItemStack(stackInSlot, 0, 27+9-1, true)) {
          return null;
        }
      } //places it into the tileEntity is possible since its in the player inventory
      else if (!this.mergeItemStack(stackInSlot, 0, 9, false)) {
        return null;
      }

      if (stackInSlot.stackSize == 0) {
        slotObject.putStack(null);
      } else {
        slotObject.onSlotChanged();
      }

      if (stackInSlot.stackSize == stack.stackSize) {
        return null;
      }
      slotObject.onPickupFromSlot(player, stackInSlot);
    }
    return stack;
  }
  
  
}
