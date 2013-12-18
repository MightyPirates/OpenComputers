package li.cil.oc

import cpw.mods.fml.common.ICraftingHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.{ItemMap, Item, ItemStack}
import net.minecraftforge.oredict.OreDictionary
import net.minecraft.world.storage.MapInfo
import li.cil.oc.server.driver.Registry

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
    if (!player.getEntityWorld.isRemote&&craftedStack.isItemEqual(Items.locator.createItemStack())) {
      for (i <- 0 to inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if(stack != null && stack.getItem== Item.map)
        {
          var map = stack.getItem.asInstanceOf[ItemMap]
          val info = map.getMapData(stack, player.getEntityWorld)

          val nbt = Registry.driverFor(craftedStack) match {
            case Some(driver)=>driver.dataTag(craftedStack)
            case _ => null
          }
          nbt.setInteger(Settings.namespace +"xCenter",info.xCenter)
          nbt.setInteger(Settings.namespace +"xCenter",info.zCenter)
          nbt.setInteger(Settings.namespace +"scale",128*(1<<info.scale))
        }


      }
    }
  }

  override def onSmelting(player: EntityPlayer, item: ItemStack) {}
}
