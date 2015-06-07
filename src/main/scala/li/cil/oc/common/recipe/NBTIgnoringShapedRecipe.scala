package li.cil.oc.common.recipe

import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapedOreRecipe

class NBTIgnoringShapedRecipe(result: ItemStack, recipe: AnyRef*) extends ShapedOreRecipe(result, recipe: _*) {

}
