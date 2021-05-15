package li.cil.oc.common.recipe

import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.mutable.ListBuffer

class ExtendedFuzzyShapelessRecipe(result: ItemStack, ingredients: AnyRef*) extends ExtendedShapelessOreRecipe(result, ingredients: _*) {
  override def matches(inv: net.minecraft.inventory.InventoryCrafting, world: net.minecraft.world.World): Boolean = {
    val requiredItems = ingredients.map(any => any.asInstanceOf[ItemStack]).toList.to[ListBuffer]
      //.groupBy{ case s: ItemStack => s.getItem }.mapValues(_.size).toSeq: _*)
    for (i <- 0 until inv.getSizeInventory) {
      val itemStack = inv.getStackInSlot(i)
      if (!itemStack.isEmpty) {
        val index = requiredItems.indexWhere(req => {
          if (req.getItem != itemStack.getItem) return false
          req.getItemDamage == itemStack.getItemDamage
        })
        if (index >= 0) {
          requiredItems.remove(index)
        }
      }
    }
    requiredItems.isEmpty
  }
}
