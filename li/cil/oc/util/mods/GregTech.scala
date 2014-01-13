package li.cil.oc.util.mods

import net.minecraft.item.ItemStack

object GregTech {
  private val (recipeAdder, addAssemblerRecipe) = try {
    val api = Class.forName("gregtechmod.api.GregTech_API")
    val iRecipe = Class.forName("gregtechmod.api.interfaces.IGT_RecipeAdder")

    val recipeAdder = api.getField("sRecipeAdder").get(null)
    val addAssemblerRecipe = iRecipe.getMethods.find(_.getName == "addAssemblerRecipe")

    (Option(recipeAdder), addAssemblerRecipe)
  }
  catch {
    case _: Throwable => (None, None)
  }

  def available = recipeAdder.isDefined

  def addAssemblerRecipe(input1: ItemStack, input2: ItemStack, output: ItemStack, duration: Int, euPerTick: Int) {
    (recipeAdder, addAssemblerRecipe) match {
      case (Some(instance), Some(method)) => method.invoke(instance, input1, input2, output, Int.box(duration), Int.box(euPerTick))
      case _ =>
    }
  }
}
