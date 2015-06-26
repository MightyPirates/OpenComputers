package li.cil.oc.integration.gregtech

import java.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.recipe.Recipes.RecipeException
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object RecipeHandler {
  def init(): Unit = {
    Recipes.registerRecipeHandler("gt_alloySmelter", addGTAlloySmelterRecipe)
    Recipes.registerRecipeHandler("gt_assembler", addGTAssemblingMachineRecipe)
    Recipes.registerRecipeHandler("gt_bender", addGTBenderRecipe)
    Recipes.registerRecipeHandler("gt_canner", addGTCannerRecipe)
    Recipes.registerRecipeHandler("gt_chemical", addGTChemicalRecipe)
    Recipes.registerRecipeHandler("gt_cnc", addGTCNCRecipe)
    Recipes.registerRecipeHandler("gt_cutter", addGTCutterRecipe)
    Recipes.registerRecipeHandler("gt_fluidCanner", addGTFluidCannerRecipe)
    Recipes.registerRecipeHandler("gt_formingPress", addGTFormingPressRecipe)
    Recipes.registerRecipeHandler("gt_lathe", addGTLatheRecipe)
    Recipes.registerRecipeHandler("gt_laserEngraver", addGTLaserEngraverRecipe)
    Recipes.registerRecipeHandler("gt_wiremill", addGTWireMillRecipe)
  }

  def addGTAlloySmelterRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, _, _, _, eu, duration) = parseRecipe(output, recipe)
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryInput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addAlloySmelterRecipe(primaryInput, secondaryInput, output, duration, eu)
        }
      case _ =>
        for (primaryInput <- primaryInputs) {
          gregtech.api.GregTech_API.sRecipeAdder.addAlloySmelterRecipe(primaryInput, null, output, duration, eu)
        }
    }
  }

  def addGTAssemblingMachineRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, fluidInput, _, _, eu, duration) = parseRecipe(output, recipe)
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryInput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(primaryInput, secondaryInput, fluidInput.orNull, output, duration, eu)
        }
      case _ =>
        for (primaryInput <- primaryInputs) {
          gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(primaryInput, null, fluidInput.orNull, output, duration, eu)
        }
    }
  }

  def addGTBenderRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, _, _, _, _, eu, duration) = parseRecipe(output, recipe)
    for (primaryInput <- primaryInputs) {
      gregtech.api.GregTech_API.sRecipeAdder.addBenderRecipe(primaryInput, output, duration, eu)
    }
  }

  def addGTCannerRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, _, _, secondaryOutputs, eu, duration) = parseRecipe(output, recipe)
    val secondaryOutput = secondaryOutputs.headOption.orNull
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryInput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addCannerRecipe(primaryInput, secondaryInput, output, secondaryOutput, duration, eu)
        }
      case None =>
        for (primaryInput <- primaryInputs) {
          gregtech.api.GregTech_API.sRecipeAdder.addCannerRecipe(primaryInput, null, output, secondaryOutput, duration, eu)
        }
    }
  }

  def addGTChemicalRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, fluidInput, fluidOutput, _, _, duration) = parseRecipe(output, recipe)
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryOutput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(primaryInput, secondaryOutput, fluidInput.orNull, fluidOutput.orNull, output, duration)
        }
      case _ =>
        for (primaryInput <- primaryInputs) {
          gregtech.api.GregTech_API.sRecipeAdder.addChemicalRecipe(primaryInput, null, fluidInput.orNull, fluidOutput.orNull, output, duration)
        }
    }
  }

  def addGTCNCRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, _, _, _, _, eu, duration) = parseRecipe(output, recipe)
    for (primaryInput <- primaryInputs) {
      gregtech.api.GregTech_API.sRecipeAdder.addCNCRecipe(primaryInput, output, duration, eu)
    }
  }

  def addGTCutterRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, _, fluidInput, _, secondaryOutputs, eu, duration) = parseRecipe(output, recipe)
    val secondaryOutput = secondaryOutputs.headOption.orNull
    fluidInput match {
      case Some(fluid) =>
        for (primaryInput <- primaryInputs) {
          gregtech.api.GregTech_API.sRecipeAdder.addCutterRecipe(primaryInput, fluid, output, secondaryOutput, duration, eu)
        }
      case _ =>
        for (primaryInput <- primaryInputs) {
          gregtech.api.GregTech_API.sRecipeAdder.addCutterRecipe(primaryInput, output, secondaryOutput, duration, eu)
        }
    }
  }

  def addGTFluidCannerRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, fluidInput, fluidOutput, _, _, _) = parseRecipe(output, recipe)
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryOutput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addFluidCannerRecipe(primaryInput, output, fluidInput.orNull, fluidOutput.orNull)
        }
      //all values required
      case _ =>
    }
  }

  def addGTFormingPressRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, _, _, _, eu, duration) = parseRecipe(output, recipe)
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryInput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addFormingPressRecipe(primaryInput, secondaryInput, output, duration, eu)
        }
      //all values required
      case _ =>
    }
  }

  def addGTLatheRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, _, _, _, secondaryOutputs, eu, duration) = parseRecipe(output, recipe)
    val secondaryOutput = secondaryOutputs.headOption.orNull
    for (primaryInput <- primaryInputs) {
      gregtech.api.GregTech_API.sRecipeAdder.addLatheRecipe(primaryInput, output, secondaryOutput, duration, eu)
    }
  }

  def addGTLaserEngraverRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, secondaryInputs, _, _, _, eu, duration) = parseRecipe(output, recipe)
    secondaryInputs match {
      case Some(value) =>
        for (primaryInput <- primaryInputs; secondaryInput <- value) {
          gregtech.api.GregTech_API.sRecipeAdder.addLaserEngraverRecipe(primaryInput, secondaryInput, output, duration, eu)
        }
      case _ =>
    }
  }

  def addGTWireMillRecipe(output: ItemStack, recipe: Config) {
    val (primaryInputs, _, _, _, _, eu, duration) = parseRecipe(output, recipe)
    for (primaryInput <- primaryInputs) {
      gregtech.api.GregTech_API.sRecipeAdder.addWiremillRecipe(primaryInput, output, duration, eu)
    }
  }

  private def parseRecipe(output: ItemStack, recipe: Config) = {
    val inputs = parseIngredientList(recipe.getValue("input")).toBuffer
    output.stackSize = Recipes.tryGetCount(recipe)

    if (inputs.size < 1 || inputs.size > 2) {
      throw new RecipeException(s"Invalid recipe length: ${inputs.size}, should be 1 or 2.")
    }

    val inputCount = recipe.getIntList("count")
    if (inputCount.size() != inputs.size) {
      throw new RecipeException(s"Mismatched ingredient count: ${inputs.size} != ${inputCount.size}.")
    }

    (inputs, inputCount).zipped.foreach((stacks, count) =>
      stacks.foreach(stack =>
        if (stack != null && count > 0)
          stack.stackSize = stack.getMaxStackSize min count))

    inputs.padTo(2, null)

    val outputs =
      if (recipe.hasPath("secondaryOutput")) {
        val secondaryOutput = parseIngredientList(recipe.getValue("secondaryOutput")).map(_.headOption)

        val outputCount = recipe.getIntList("secondaryOutputCount")
        if (outputCount.size() != secondaryOutput.size) {
          throw new RecipeException(s"Mismatched secondary output count: ${secondaryOutput.size} != ${outputCount.size}.")
        }

        (secondaryOutput, outputCount).zipped.foreach((stack, count) =>
          if (count > 0) stack.foreach(s => s.stackSize = s.getMaxStackSize min count))
        secondaryOutput.collect { case Some(stack) => stack }
      }
      else Iterable.empty[ItemStack]

    val inputFluidStack =
      if (recipe.hasPath("inputFluid")) Recipes.parseFluidIngredient(recipe.getConfig("inputFluid"))
      else None

    val outputFluidStack =
      if (recipe.hasPath("outputFluid")) Recipes.parseFluidIngredient(recipe.getConfig("outputFluid"))
      else None

    val eu = recipe.getInt("eu")
    val duration = recipe.getInt("time")

    (inputs.head, Option(inputs.last), inputFluidStack, outputFluidStack, outputs, eu, duration)
  }

  private def parseIngredientList(list: ConfigValue) =
    (list.unwrapped() match {
      case list: util.List[AnyRef]@unchecked => list.map(Recipes.parseIngredient)
      case other => Iterable(Recipes.parseIngredient(other))
    }) map {
      case null => Array.empty[ItemStack]
      case stack: ItemStack => Array(stack)
      case name: String => Array(OreDictionary.getOres(name): _*)
      case other => throw new RecipeException(s"Invalid ingredient type: $other.")
    }
}
