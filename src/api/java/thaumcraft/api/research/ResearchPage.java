package thaumcraft.api.research;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;

public class ResearchPage {
	public static enum PageType
    {
        TEXT,
        TEXT_CONCEALED,
        IMAGE,
        CRUCIBLE_CRAFTING,
        ARCANE_CRAFTING,
        ASPECTS,
        NORMAL_CRAFTING,
        INFUSION_CRAFTING,
        COMPOUND_CRAFTING,
        INFUSION_ENCHANTMENT
    }
	
	public PageType type = PageType.TEXT;
	
	public String text=null;
	public String research=null;
	public ResourceLocation image=null;
	public AspectList aspects=null;
	public Object recipe=null;
	public ItemStack recipeOutput=null;
	
	/**
	 * @param text this can (but does not have to) be a reference to a localization variable, not the actual text.
	 */
	public ResearchPage(String text) {
		this.type = PageType.TEXT;
		this.text = text;
	}
	
	/**
	 * @param research this page will only be displayed if the player has discovered this research
	 * @param text this can (but does not have to) be a reference to a localization variable, not the actual text.
	 */
	public ResearchPage(String research, String text) {
		this.type = PageType.TEXT_CONCEALED;
		this.research = research;
		this.text = text;
	}
	
	/**
	 * @param recipe a vanilla crafting recipe.
	 */
	public ResearchPage(IRecipe recipe) {
		this.type = PageType.NORMAL_CRAFTING;
		this.recipe = recipe;
		this.recipeOutput = recipe.getRecipeOutput();
	}
	
	/**
	 * @param recipe a collection of vanilla crafting recipes.
	 */
	public ResearchPage(IRecipe[] recipe) {
		this.type = PageType.NORMAL_CRAFTING;
		this.recipe = recipe;
	}
	
	/**
	 * @param recipe a collection of arcane crafting recipes.
	 */
	public ResearchPage(IArcaneRecipe[] recipe) {
		this.type = PageType.ARCANE_CRAFTING;
		this.recipe = recipe;
	}
	
	/**
	 * @param recipe a collection of infusion crafting recipes.
	 */
	public ResearchPage(InfusionRecipe[] recipe) {
		this.type = PageType.INFUSION_CRAFTING;
		this.recipe = recipe;
	}
	
	/**
	 * @param recipe a compound crafting recipe.
	 */
	public ResearchPage(List recipe) {
		this.type = PageType.COMPOUND_CRAFTING;
		this.recipe = recipe;
	}
	
	/**
	 * @param recipe an arcane worktable crafting recipe.
	 */
	public ResearchPage(IArcaneRecipe recipe) {
		this.type = PageType.ARCANE_CRAFTING;
		this.recipe = recipe;
		this.recipeOutput = recipe.getRecipeOutput();
	}
	
	/**
	 * @param recipe an alchemy crafting recipe.
	 */
	public ResearchPage(CrucibleRecipe recipe) {
		this.type = PageType.CRUCIBLE_CRAFTING;
		this.recipe = recipe;
		this.recipeOutput = recipe.getRecipeOutput();
	}
	
	/**
	 * @param recipe an infusion crafting recipe.
	 */
	public ResearchPage(InfusionRecipe recipe) {
		this.type = PageType.INFUSION_CRAFTING;
		this.recipe = recipe;
		if (recipe.getRecipeOutput() instanceof ItemStack) {
			this.recipeOutput = (ItemStack) recipe.getRecipeOutput();
		} else {
			this.recipeOutput = recipe.getRecipeInput();
		}
	}
	
	/**
	 * @param recipe an infusion crafting recipe.
	 */
	public ResearchPage(InfusionEnchantmentRecipe recipe) {
		this.type = PageType.INFUSION_ENCHANTMENT;
		this.recipe = recipe;
//		if (recipe.recipeOutput instanceof ItemStack) {
//			this.recipeOutput = (ItemStack) recipe.recipeOutput;
//		} else {
//			this.recipeOutput = recipe.recipeInput;
//		}
	}
	
	/**
	 * @param image
	 * @param caption this can (but does not have to) be a reference to a localization variable, not the actual text.
	 */
	public ResearchPage(ResourceLocation image, String caption) {
		this.type = PageType.IMAGE;
		this.image = image;
		this.text = caption;
	}
	
	/**
	 * This function should really not be called directly - used internally
	 */
	public ResearchPage(AspectList as) {
		this.type = PageType.ASPECTS;
		this.aspects = as;
	}
	
	/**
	 * returns a localized text of the text field (if one exists). Returns the text field itself otherwise.
	 * @return
	 */
	public String getTranslatedText() {
		String ret="";
		if (text != null) {
			ret = StatCollector.translateToLocal(text);
			if (ret.isEmpty()) ret = text;
		}
		return ret;
	}
	
	
}
