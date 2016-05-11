package li.cil.oc.common.recipe

import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.common.ForgeHooks

trait ContainerItemAwareRecipe extends IRecipe {
  override def getRemainingItems(inv: InventoryCrafting) = ForgeHooks.defaultRecipeGetRemainingItems(inv)
}
