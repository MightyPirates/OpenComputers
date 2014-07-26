/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.fuels;

import java.util.HashMap;


public class FuelManager {
	/**
	 * Add new fuels for the fermenter here (i.e. fertilizer). Will accept Items, ItemStacks and Strings (Ore Dictionary)
	 */
	public static HashMap<Object, FermenterFuel> fermenterFuel;
	/**
	 * Add new resources for the moistener here (i.e. wheat)
	 */
	public static HashMap<Object, MoistenerFuel> moistenerResource;
	/**
	 * Add new substrates for the rainmaker here
	 */
	public static HashMap<Object, RainSubstrate> rainSubstrate;
	/**
	 * Add new fuels for EngineBronze (= biogas engine) here
	 */
	public static HashMap<Object, EngineBronzeFuel> bronzeEngineFuel;
	/**
	 * Add new fuels for EngineCopper (= peat-fired engine) here
	 */
	public static HashMap<Object, EngineCopperFuel> copperEngineFuel;

	// Generator fuel list in GeneratorFuel.class
}
