package li.cil.oc

import cpw.mods.fml.common.ICraftingHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.{Item, ItemStack}

object CraftingHandler extends ICraftingHandler {
  override def onCrafting(player: EntityPlayer, craftedStack: ItemStack, inventory: IInventory) = {
    if (craftedStack.isItemEqual(Items.acid.createItemStack())) {
      for (i <- 0 to inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.getItem == Item.bucketWater) {
          stack.stackSize = 0
          inventory.setInventorySlotContents(i, null)
        }
      }
    }

    if (craftedStack.isItemEqual(Items.pcb.createItemStack())) {
      for (i <- 0 to inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.isItemEqual(Items.acid.createItemStack())) {
          val container = new ItemStack(Item.bucketEmpty, 1)
          if (!player.inventory.addItemStackToInventory(container)) {
            player.dropPlayerItem(container)
          }
        }
      }
    }
  }

  override def onSmelting(player: EntityPlayer, item: ItemStack) {}
}
