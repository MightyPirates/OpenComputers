package appeng.api.recipes;

/**
 * Represents the AE2 Recipe Loading/Reading Class
 */
public interface IRecipeHandler
{

	/**
	 * Call when you want to read recipes in from a file based on a loader
	 * 
	 * @param loader
	 * @param path
	 */
	void parseRecipes(IRecipeLoader loader, String path);

	/**
	 * this loads the read recipes into minecraft.
	 */
	void registerHandlers();

}
