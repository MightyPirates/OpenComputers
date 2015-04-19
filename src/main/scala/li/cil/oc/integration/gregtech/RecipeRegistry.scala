package li.cil.oc.integration.gregtech

import java.util

import com.typesafe.config.Config
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.recipe.Recipes.RecipeException
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object RecipeRegistry {

  def getGTRecipesWithEU(output: ItemStack, recipe: Config): (Array[ItemStack], Option[Array[ItemStack]], Option[FluidStack], Option[FluidStack], Seq[ItemStack], Int, Int) = {

    val inputs = (recipe.getValue("input").unwrapped() match {
      case list: util.List[AnyRef]@unchecked => list.map(Recipes.parseIngredient)
      case other => Seq(Recipes.parseIngredient(other))
    }) map {
      case null => Array.empty[ItemStack]
      case stack: ItemStack => Array(stack)
      case name: String => Array(OreDictionary.getOres(name): _*)
      case other => throw new RecipeException(s"Invalid ingredient type: $other.")
    }
    output.stackSize = Recipes.tryGetCount(recipe)

    if (inputs.size < 1 || inputs.size > 2) {
      throw new RecipeException(s"Invalid recipe length: ${inputs.size}, should be 1 or 2.")
    }

    val inputCount = recipe.getIntList("count")
    if (inputCount.size() != inputs.size) {
      throw new RecipeException(s"Ingredient and input count mismatch: ${inputs.size} != ${inputCount.size}.")
    }

    var inputFluidStack: Option[FluidStack] = None: Option[FluidStack]
    if (recipe.hasPath("inputLiquid")) Recipes.parseFluidIngredient(recipe.getString("inputLiquid")) match {
      case Some(fluid) =>
        var inputFluidAmount = 1000
        if (recipe.hasPath("inputFluidAmount")) inputFluidAmount = recipe.getInt("inputFluidAmount")
        inputFluidStack = Option(new FluidStack(fluid, inputFluidAmount))
      case _ =>
    }

    var outputFluidStack: Option[FluidStack] = None: Option[FluidStack]
    if (recipe.hasPath("outputLiquid")) Recipes.parseFluidIngredient(recipe.getString("outputLiquid")) match {
      case Some(fluid) =>
        var fluidAmount = 1000
        if (recipe.hasPath("outputFluidAmount")) fluidAmount = recipe.getInt("outputFluidAmount")
        outputFluidStack = Option(new FluidStack(fluid, fluidAmount))
      case _ =>
    }

    val eu = recipe.getInt("eu")
    val duration = recipe.getInt("time")
    var additionalOutput = Seq.empty[ItemStack]
    if (recipe.hasPath("additionalOutput")) {
      additionalOutput = (recipe.getValue("additionalOutput").unwrapped() match {
        case list: util.List[AnyRef]@unchecked => list.map(Recipes.parseIngredient)
        case other => Seq(Recipes.parseIngredient(other))
      }) map {
        case stack: ItemStack => stack
        case name: String => val ores = OreDictionary.getOres(name)
          if (ores.size() > 0)
            ores.get(0)
          else
            null
        case other => throw new RecipeException(s"Invalid ingredient type: $other.")
      }

      val outputCount = recipe.getIntList("outputCount")
      if (outputCount.size() != additionalOutput.size) {
        throw new RecipeException(s"Outputs and output count mismatch: ${additionalOutput.size} != ${outputCount.size}.")
      }
      (additionalOutput, outputCount).zipped.foreach((stack, count) => if (stack != null && count > 0) stack.stackSize = stack.getMaxStackSize min count)
    }


    (inputs, inputCount).zipped.foreach((stacks, count) => stacks.foreach(stack => if (stack != null && count > 0) stack.stackSize = stack.getMaxStackSize min count))



    inputs.padTo(2, null)
    val input = inputs.head
    (input, Option(inputs.last), inputFluidStack, outputFluidStack, additionalOutput, eu, duration)
  }

  def addGTAlloySmelterRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    for (input1 <- inputs1) {
      inputs2 match {
        case Some(inputs2List) =>
          for (input2 <- inputs2List)
            gregtech.api.GregTech_API.sRecipeAdder.addAlloySmelterRecipe(input1, input2, output, duration, eu)
        case None =>
          gregtech.api.GregTech_API.sRecipeAdder.addAlloySmelterRecipe(input1, null, output, duration, eu)
      }
    }
  }

  def addGTAssemblingMachineRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    for (input1 <- inputs1) {
      inputs2 match {
        case Some(inputs2List) =>
          for (input2 <- inputs2List)
            fluidStackIn match {
              case Some(fluid) =>

                gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(input1, input2, fluid, output, duration, eu)
              case None =>
                gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(input1, input2, output, duration, eu)
            }

        case None =>
          fluidStackIn match {
            case Some(fluid) =>
              gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(input1, null, fluid, output, duration, eu)
            case None =>
              gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(input1, null, output, duration, eu)
          }
      }
    }
  }

  def addGTBenderRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    for (input1 <- inputs1) {
      gregtech.api.GregTech_API.sRecipeAdder.addBenderRecipe(input1, output, duration, eu)
    }
  }

  def addGTCutterRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    var add: ItemStack = null
    if (additionalOutput.size > 1)
      add = additionalOutput.head
    for (input1 <- inputs1) {
      fluidStackIn match {
        case Some(fluid) =>
          gregtech.api.GregTech_API.sRecipeAdder.addCutterRecipe(input1, fluid, output, add, duration, eu)

        case None =>
          gregtech.api.GregTech_API.sRecipeAdder.addCutterRecipe(input1, output, add, duration, eu)
      }
    }
  }

  def addGTCannerRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    var add: ItemStack = null
    if (additionalOutput.size > 1)
      add = additionalOutput.head

    for (input1 <- inputs1) {
      inputs2 match {
        case Some(inputs2List) =>
          for (input2 <- inputs2List)
            gregtech.api.GregTech_API.sRecipeAdder.addCannerRecipe(input1, input2, output, add, duration, eu)
        case None =>
          gregtech.api.GregTech_API.sRecipeAdder.addCannerRecipe(input1, null, output, add, duration, eu)


      }
    }
  }

  def addGTChemicalRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    for (input1 <- inputs1) {
      inputs2 match {
        case Some(inputs2List) =>
          for (input2 <- inputs2List)
            fluidStackIn match {
              case Some(fluid) =>
                fluidStackOut match {
                  case Some(fluidOut) =>
                    gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, input2, fluid, fluidOut, output, duration)

                  case _ =>
                    gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, input2, fluid, null, output, duration)
                }
              case None =>
                fluidStackOut match {
                  case Some(fluidOut) =>
                    gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, input2, null, fluidOut, output, duration)
                  case _ =>
                    gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, input2, output, duration)
                }
            }

        case None =>
          fluidStackIn match {
            case Some(fluid) =>
              fluidStackOut match {
                case Some(fluidOut) =>
                  gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, null, fluid, fluidOut, output, duration)

                case _ =>
                  gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, null, fluid, null, output, duration)
              }
            case None =>
              fluidStackOut match {
                case Some(fluidOut) =>
                  gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, null, null, fluidOut, output, duration)
                case _ =>
                  gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(input1, null, output, duration)
              }
          }
      }
    }
  }

  def addGTCNCRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    for (input1 <- inputs1) {
      gregtech.api.GregTech_API.sRecipeAdder.addCNCRecipe(input1, output, duration, eu)
    }
  }

  def addGTLatheRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    var add: ItemStack = null
    if (additionalOutput.size > 1)
      add = additionalOutput.head
    for (input1 <- inputs1) {
      gregtech.api.GregTech_API.sRecipeAdder.addLatheRecipe(input1, output, add, duration, eu)
    }
  }

  def addGTWireMillRecipe(output: ItemStack, recipe: Config) {
    val (inputs1, inputs2, fluidStackIn, fluidStackOut, additionalOutput, eu, duration) = getGTRecipesWithEU(output, recipe)
    for (input1 <- inputs1) {
      gregtech.api.GregTech_API.sRecipeAdder.addWiremillRecipe(input1, output, duration, eu)
    }
  }


  def init(): Unit = {
    Recipes.registerRecipe("gt_alloySmelter", addGTAlloySmelterRecipe)
    Recipes.registerRecipe("gt_assembler", addGTAssemblingMachineRecipe)
    Recipes.registerRecipe("gt_bender", addGTBenderRecipe)
    Recipes.registerRecipe("gt_canner", addGTCannerRecipe)
    Recipes.registerRecipe("gt_chemical", addGTChemicalRecipe)
    Recipes.registerRecipe("gt_cnc", addGTCNCRecipe)
    Recipes.registerRecipe("gt_cutter", addGTCutterRecipe)
    Recipes.registerRecipe("gt_lathe", addGTLatheRecipe)
    Recipes.registerRecipe("gt_wiremill", addGTWireMillRecipe)

  }
}
