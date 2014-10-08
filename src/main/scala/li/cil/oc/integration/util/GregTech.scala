package li.cil.oc.integration.util

import net.minecraft.item.ItemStack

object GregTech {
  private val (recipeAdder, addAssemblerRecipe) = try {
    val api = Class.forName("gregtech.api.GregTech_API")
    val recipeAdder = api.getField("sRecipeAdder").get(null)
    val addAssemblerRecipe = recipeAdder.getClass.getMethod("addAssemblerRecipe", classOf[ItemStack], classOf[ItemStack], classOf[ItemStack], classOf[Int], classOf[Int])

    (Option(recipeAdder), Option(addAssemblerRecipe))
  }
  catch {
    case _: Throwable => (None, None)
  }

  def available = recipeAdder.isDefined && addAssemblerRecipe.isDefined

  def addAssemblerRecipe(input1: ItemStack, input2: ItemStack, output: ItemStack, duration: Int, euPerTick: Int) {
    (recipeAdder, addAssemblerRecipe) match {
      case (Some(instance), Some(method)) => method.invoke(instance, input1, input2, output, Int.box(duration), Int.box(euPerTick))
      case _ =>
    }
  }
}
