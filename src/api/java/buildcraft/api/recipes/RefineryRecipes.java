/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.recipes;

import com.google.common.base.Objects;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import net.minecraftforge.fluids.FluidStack;

public final class RefineryRecipes {

	private static SortedSet<Recipe> recipes = new TreeSet<Recipe>();

	public static void addRecipe(FluidStack ingredient, FluidStack result, int energy, int delay) {
		addRecipe(ingredient, null, result, energy, delay);
	}

	public static void addRecipe(FluidStack ingredient1, FluidStack ingredient2, FluidStack result, int energy, int delay) {
		Recipe recipe = new Recipe(ingredient1, ingredient2, result, energy, delay);
		recipes.add(recipe);
	}

	public static SortedSet<Recipe> getRecipes() {
		return Collections.unmodifiableSortedSet(recipes);
	}

	public static Recipe findRefineryRecipe(FluidStack liquid1, FluidStack liquid2) {
		for (Recipe recipe : recipes) {
			if (recipe.matches(liquid1, liquid2))
				return recipe;
		}

		return null;
	}

	private RefineryRecipes() {
	}

	public static final class Recipe implements Comparable<Recipe> {

		public final FluidStack ingredient1;
		public final FluidStack ingredient2;
		public final FluidStack result;
		public final int energy;
		public final int delay;

		private Recipe(FluidStack ingredient1, FluidStack ingredient2, FluidStack result, int energy, int delay) {
			if (ingredient1 == null)
				throw new IllegalArgumentException("First Ingredient cannot be null!");
			this.ingredient1 = ingredient1;
			this.ingredient2 = ingredient2;
			this.result = result;
			this.energy = energy;
			this.delay = delay;
		}

		public boolean matches(FluidStack liquid1, FluidStack liquid2) {

			// No inputs, return.
			if (liquid1 == null && liquid2 == null)
				return false;

			// Return if two ingredients are required but only one was supplied.
			if ((ingredient1 != null && ingredient2 != null) && (liquid1 == null || liquid2 == null))
				return false;

			if (liquid1 != null && liquid2 != null) {
				if (liquid1.containsFluid(ingredient1) && liquid1.containsFluid(ingredient2))
					return true;
				if (liquid1.containsFluid(ingredient2) && liquid1.containsFluid(ingredient1))
					return true;
			}

			if (liquid1 != null)
				return liquid1.containsFluid(ingredient1) || liquid1.containsFluid(ingredient2);

			if (liquid2 != null)
				return liquid2.containsFluid(ingredient1) || liquid2.containsFluid(ingredient2);

			return false;
		}

		// Compares to only the types of source materials.
		// We consider non-null < null in order that one-ingredient recipe is checked after
		// the failure of matching two-ingredient recipes which include that liquid.
		@Override
		public int compareTo(Recipe other) {
			if (other == null)
				return -1;
			else if (ingredient1.getFluid() != other.ingredient1.getFluid())
				return ingredient1.getFluid().getName().compareTo(other.ingredient1.getFluid().getName());
			else if (ingredient1.amount != other.ingredient1.amount)
				return other.ingredient1.amount - ingredient1.amount;
			else if (ingredient2 == null)
				return other.ingredient2 == null ? 0 : 1;
			else if (other.ingredient2 == null)
				return -1;
			else if (ingredient2.getFluid() != other.ingredient2.getFluid())
				return ingredient2.getFluid().getName().compareTo(other.ingredient2.getFluid().getName());
			else if (ingredient2.amount != other.ingredient2.amount)
				return other.ingredient2.amount - ingredient2.amount;

			return 0;
		}

		// equals() should be consistent with compareTo().
		@Override
		public boolean equals(Object obj) {
			return obj instanceof Recipe
					&& Objects.equal(ingredient1, ((Recipe) obj).ingredient1)
					&& Objects.equal(ingredient2, ((Recipe) obj).ingredient2);
		}

		// hashCode() should be overridden because equals() was overridden.
		@Override
		public int hashCode() {
			return Objects.hashCode(ingredient1, ingredient2);
		}
	}
}
