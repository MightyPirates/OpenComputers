package li.cil.oc.common.recipe

import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.ShapedOreRecipe

class ExtendedShapedOreRecipe(result: ItemStack, ingredients: AnyRef*) extends ShapedOreRecipe(null, result, ingredients: _*) {
  override def getCraftingResult(inventory: InventoryCrafting) =
    ExtendedRecipe.addNBTToResult(this, super.getCraftingResult(inventory), inventory)
}
