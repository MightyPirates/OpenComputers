package li.cil.oc

import cpw.mods.fml.common.ICraftingHandler
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory


class CraftingHandler extends ICraftingHandler {
  /**
   * The object array contains these three arguments
   *
   * @param player
   * @param item
   * @param craftMatrix
   */
  override def onCrafting(player: EntityPlayer, item: ItemStack, craftMatrix: IInventory) = {
    if (item.isItemEqual(Items.circuitBoardBody.createItemStack())) {
      for ( i <- 0 to craftMatrix.getSizeInventory)
      {
        val itemStack = craftMatrix.getStackInSlot(i)

        if (itemStack != null && itemStack.getItem() == Item.potion) {
          println(itemStack)
          val stack = new ItemStack(Item.glassBottle,1)
          if(!player.inventory.addItemStackToInventory(stack))
            player.dropPlayerItem(stack)
        }
      }
    }
    else if(item.isItemEqual(Items.ironCutter.createItemStack())){
      for ( i <- 0 to craftMatrix.getSizeInventory)
      {
        val itemStack = craftMatrix.getStackInSlot(i)
        if (itemStack != null && itemStack.getItem == Item.shears) {
          println(itemStack)
          itemStack.damageItem(20, player)
          itemStack.stackSize = itemStack.stackSize+1
          println(itemStack)
        }
      }
    }
  }

  /**
   * The object array contains these two arguments
   * @param player
   * @param item
   */
  override def onSmelting(player: EntityPlayer, item: ItemStack) = {

  }
}
