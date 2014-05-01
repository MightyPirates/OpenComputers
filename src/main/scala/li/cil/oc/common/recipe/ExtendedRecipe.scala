package li.cil.oc.common.recipe

import net.minecraft.item.{Item, ItemStack}
import li.cil.oc.{Settings, api}
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import cpw.mods.fml.common.FMLCommonHandler
import java.util.UUID
import net.minecraft.inventory.InventoryCrafting

object ExtendedRecipe {
  private lazy val navigationUpgrade = api.Items.get("navigationUpgrade")
  private lazy val linkedCard = api.Items.get("linkedCard")

  def addNBTToResult(craftedStack: ItemStack, inventory: InventoryCrafting) = {
    if (api.Items.get(craftedStack) == navigationUpgrade) {
      Registry.itemDriverFor(craftedStack) match {
        case Some(driver) =>
          for (i <- 0 until inventory.getSizeInventory) {
            val stack = inventory.getStackInSlot(i)
            if (stack != null && stack.getItem == Item.map) {
              // Store information of the map used for crafting in the result.
              val nbt = driver.dataTag(craftedStack)
              nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
            }
          }
        case _ =>
      }
    }

    if (api.Items.get(craftedStack) == linkedCard && FMLCommonHandler.instance.getEffectiveSide.isServer) {
      Registry.itemDriverFor(craftedStack) match {
        case Some(driver) =>
          val nbt = driver.dataTag(craftedStack)
          nbt.setString(Settings.namespace + "tunnel", UUID.randomUUID().toString)
        case _ =>
      }
    }

    craftedStack
  }
}
