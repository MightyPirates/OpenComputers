package appeng.api.recipes;

import java.util.List;

import appeng.api.exceptions.MissingIngredientError;
import appeng.api.exceptions.RecipeError;
import appeng.api.exceptions.RegistrationError;

public interface ICraftHandler
{

	/**
	 * Called when your recipe handler receives a newly parsed list of inputs/outputs.
	 * 
	 * @param input
	 * @param output
	 * @throws RecipeError
	 */
	public void setup(List<List<IIngredient>> input, List<List<IIngredient>> output) throws RecipeError;

	/**
	 * called when all recipes are parsed, and your required to register your recipe.
	 * 
	 * @throws RegistrationError
	 * @throws MissingIngredientError
	 */
	public void register() throws RegistrationError, MissingIngredientError;

}
