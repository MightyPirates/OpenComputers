package li.cil.oc

import cpw.mods.fml.common.ICraftingHandler
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.{ItemMap, Item, ItemStack}

object CraftingHandler extends ICraftingHandler {
  lazy val acid = api.Items.get("acid").createItemStack(1)
  lazy val pcb = api.Items.get("printedCircuitBoard").createItemStack(1)
  lazy val navigationUpgrade = api.Items.get("navigationUpgrade").createItemStack(1)

  override def onCrafting(player: EntityPlayer, craftedStack: ItemStack, inventory: IInventory) = {
    if (craftedStack.isItemEqual(acid)) {
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.getItem == Item.bucketWater) {
          stack.stackSize = 0
          inventory.setInventorySlotContents(i, null)
        }
      }
    }

    if (craftedStack.isItemEqual(pcb)) {
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null && stack.isItemEqual(acid)) {
          val container = new ItemStack(Item.bucketEmpty, 1)
          if (!player.inventory.addItemStackToInventory(container)) {
            player.dropPlayerItem(container)
          }
        }
      }
    }

    if (craftedStack.isItemEqual(navigationUpgrade)) {
      Registry.itemDriverFor(craftedStack) match {
        case Some(driver) =>
          var oldMap = None: Option[ItemStack]
          for (i <- 0 until inventory.getSizeInventory) {
            val stack = inventory.getStackInSlot(i)
            if (stack != null) {
              if (stack.isItemEqual(navigationUpgrade)) {
                // Restore the map currently used in the upgrade.
                val nbt = driver.dataTag(stack)
                oldMap = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map")))
              }
              else if (stack.getItem == Item.map) {
                // Store information of the map used for crafting in the result.
                val nbt = driver.dataTag(craftedStack)
                val map = stack.getItem.asInstanceOf[ItemMap]
                val info = map.getMapData(stack, player.getEntityWorld)
                nbt.setInteger(Settings.namespace + "xCenter", info.xCenter)
                nbt.setInteger(Settings.namespace + "zCenter", info.zCenter)
                nbt.setInteger(Settings.namespace + "scale", 128 * (1 << info.scale))
                nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
              }
            }
          }
          if (oldMap.isDefined) {
            val map = oldMap.get
            if (!player.inventory.addItemStackToInventory(map)) {
              player.dropPlayerItemWithRandomChoice(map, false)
            }
          }
        case _ =>
      }
    }
  }

  override def onSmelting(player: EntityPlayer, item: ItemStack) {}
}
