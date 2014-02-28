package ic2.api.recipe;



/**
 * General recipe registry.
 * 
 * @author Richard
 */
public class Recipes {
	public static IMachineRecipeManager macerator;
	public static IMachineRecipeManager extractor;
	public static IMachineRecipeManager compressor;
	public static IMachineRecipeManager centrifuge;
	public static IMachineRecipeManager recycler;
	public static IMachineRecipeManager metalformerExtruding;
	public static IMachineRecipeManager metalformerCutting;
	public static IMachineRecipeManager metalformerRolling;
	public static IMachineRecipeManager oreWashing;
	public static IMachineRecipeManager Scanner;
	public static ICannerBottleRecipeManager cannerBottle;
	public static ICannerEnrichRecipeManager cannerEnrich;

	/**
	 * Reference amplifier values:
	 * 
	 *  5000: Scrap
	 * 45000: Scrapbox
	 * 
	 * As Parameter for the Amplification Value you have to use the NBTTagCompound
	 * 
	 * NBTTagCompound nbt = new NBTTagCompound();
	 * nbt.setInteger("amplification", aValue);
	 * matterAmplifier.addRecipe(yourStack, nbt);
	 */
	public static IMachineRecipeManager matterAmplifier;
	/**
	 * Reference scrap box chance values:
	 *
	 * 0.1: Diamond
	 * 0.5: Cake, Gold Helmet, Iron Ore, Gold Ore
	 * 1.0: Wooden tools, Soul Sand, Sign, Leather, Feather, Bone
	 * 1.5: Apple, Bread
	 * 2.0: Netherrack, Rotten Flesh
	 * 3.0: Grass, Gravel
	 * 4.0: Stick
	 * 5.0: Dirt, Wooden Hoe
	 */
	public static IScrapboxManager scrapboxDrops;
	public static IListRecipeManager recyclerBlacklist;
	/**
	 * Do not add anything to this Whitelist. This is for Configuration only.
	 * You may need this if you have an own Recycler in your Mod, just to check if something can be recycled. but don't add anything to this List
	 */
	public static IListRecipeManager recyclerWhitelist;
	public static ICraftingRecipeManager advRecipes;

	public static ISemiFluidFuelManager semiFluidGenerator;

}
