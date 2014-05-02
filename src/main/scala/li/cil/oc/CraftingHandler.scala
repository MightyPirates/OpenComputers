package li.cil.oc

import cpw.mods.fml.common.ICraftingHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.{Item, ItemStack}

object CraftingHandler extends ICraftingHandler {
  lazy val acid = api.Items.get("acid")
  lazy val pcb = api.Items.get("printedCircuitBoard")
  lazy val navigationUpgrade = api.Items.get("navigationUpgrade")

  override def onCrafting(player: EntityPlayer, craftedStack: ItemStack, inventory: IInventory) = {
    if (api.Items.get(craftedStack) == acid) {
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.getItem == Item.bucketWater) {
          stack.stackSize = 0
          inventory.setInventorySlotContents(i, null)
        }
      }
    }

    if (api.Items.get(craftedStack) == pcb) {
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (api.Items.get(stack) == acid) {
          val container = new ItemStack(Item.bucketEmpty, 1)
          if (!player.inventory.addItemStackToInventory(container)) {
            player.dropPlayerItem(container)
          }
        }
      }
    }

    if (api.Items.get(craftedStack) == navigationUpgrade) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver =>
        for (i <- 0 until inventory.getSizeInventory) {
          val stack = inventory.getStackInSlot(i)
          if (stack != null && api.Items.get(stack) == navigationUpgrade) {
            // Restore the map currently used in the upgrade.
            val nbt = driver.dataTag(stack)
            val map = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map"))
            if (!player.inventory.addItemStackToInventory(map)) {
              player.dropPlayerItemWithRandomChoice(map, false)
            }
          }
        })
    }
  }

  override def onSmelting(player: EntityPlayer, item: ItemStack) {}
}
