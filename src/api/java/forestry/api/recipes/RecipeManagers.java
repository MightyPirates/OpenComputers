/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.recipes;

import java.util.Collection;

/**
 * Contains all available recipe managers for Forestry machines and items.
 * 
 * @author SirSengir
 */
public class RecipeManagers {

	public static Collection<ICraftingProvider> craftingProviders;
	
	/**
	 * Allows you to add recipes to the bottler. See {@link IBottlerManager} for details.
	 */
	public static IBottlerManager bottlerManager;
	/**
	 * Allows you to add recipes to the carpenter. See {@link ICarpenterManager} for details.
	 */
	public static ICarpenterManager carpenterManager;
	/**
	 * Allows you to add recipes to the centrifuge. See {@link ICentrifugeManager} for details.
	 */
	public static ICentrifugeManager centrifugeManager;
	/**
	 * Allows you to add recipes to the fermenter. See {@link IFermenterManager} for details.
	 */
	public static IFermenterManager fermenterManager;
	/**
	 * Allows you to add recipes to the moistener. See {@link IMoistenerManager} for details.
	 */
	public static IMoistenerManager moistenerManager;
	/**
	 * Allows you to add recipes to the squeezer. See {@link ISqueezerManager} for details.
	 */
	public static ISqueezerManager squeezerManager;
	/**
	 * Allows you to add recipes to the still. See {@link IStillManager} for details.
	 */
	public static IStillManager stillManager;

	public static IFabricatorManager fabricatorManager;
}
