package li.cil.oc.common.recipe

import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry

trait ContainerItemAwareRecipe extends IForgeRegistryEntry.Impl[IRecipe] with IRecipe {
  override def getRemainingItems(inv: InventoryCrafting) = ForgeHooks.defaultRecipeGetRemainingItems(inv)

  def getMinimumRecipeSize: Int

  override def canFit(width: Int, height: Int): Boolean = width * height >= getMinimumRecipeSize
}
