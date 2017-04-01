package li.cil.oc.common.recipe

import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapelessOreRecipe

class ExtendedShapelessOreRecipe(result: ItemStack, ingredients: AnyRef*) extends ShapelessOreRecipe(result, ingredients: _*) {
  override def getCraftingResult(inventory: InventoryCrafting): ItemStack =
    ExtendedRecipe.addNBTToResult(this, super.getCraftingResult(inventory), inventory)
}
