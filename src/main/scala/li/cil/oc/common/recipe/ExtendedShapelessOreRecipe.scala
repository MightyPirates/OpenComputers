package li.cil.oc.common.recipe

import net.minecraftforge.oredict.ShapelessOreRecipe
import net.minecraft.item.ItemStack
import net.minecraft.inventory.InventoryCrafting

class ExtendedShapelessOreRecipe(result: ItemStack, ingredients: AnyRef*) extends ShapelessOreRecipe(result, ingredients: _*) {
  override def getCraftingResult(inventory: InventoryCrafting) =
    ExtendedRecipe.addNBTToResult(super.getCraftingResult(inventory), inventory)
}
