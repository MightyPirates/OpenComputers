package li.cil.oc.common.recipe

import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraft.item.ItemStack
import net.minecraft.inventory.InventoryCrafting

class ExtendedShapedOreRecipe(result: ItemStack, ingredients: AnyRef*) extends ShapedOreRecipe(result, ingredients: _*) {
  override def getCraftingResult(inventory: InventoryCrafting) =
    ExtendedRecipe.addNBTToResult(super.getCraftingResult(inventory), inventory)
}
