package li.cil.oc.common.recipe

import cpw.mods.fml.common.FMLCommonHandler
import java.util.UUID
import li.cil.oc.{Settings, api}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.{Item, ItemStack}

object ExtendedRecipe {
  private lazy val navigationUpgrade = api.Items.get("navigationUpgrade")
  private lazy val linkedCard = api.Items.get("linkedCard")

  def addNBTToResult(craftedStack: ItemStack, inventory: InventoryCrafting) = {
    if (api.Items.get(craftedStack) == navigationUpgrade) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver =>
        for (i <- 0 until inventory.getSizeInventory) {
          val stack = inventory.getStackInSlot(i)
          if (stack != null && stack.getItem == Item.map) {
            // Store information of the map used for crafting in the result.
            val nbt = driver.dataTag(craftedStack)
            nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
          }
        })
    }

    if (api.Items.get(craftedStack) == linkedCard && FMLCommonHandler.instance.getEffectiveSide.isServer) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver => {
        val nbt = driver.dataTag(craftedStack)
        nbt.setString(Settings.namespace + "tunnel", UUID.randomUUID().toString)
      })
    }

    craftedStack
  }
}
