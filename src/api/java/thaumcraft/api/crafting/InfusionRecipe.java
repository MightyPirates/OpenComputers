package thaumcraft.api.crafting;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;

public class InfusionRecipe
{
	
	public AspectList aspects;
	public String research;
	public ItemStack[] components;
	public ItemStack recipeInput;
	public Object recipeOutput;
	public int instability;
	
	public InfusionRecipe(String research, Object output, int inst,
			AspectList aspects2, ItemStack input, ItemStack[] recipe) {
		this.research = research;
		this.recipeOutput = output;
		this.recipeInput = input;
		this.aspects = aspects2;
		this.components = recipe;
		this.instability = inst;
	}

	/**
     * Used to check if a recipe matches current crafting inventory
     * @param player 
     */
	public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
		if (recipeInput==null) return false;
			
		if (research.length()>0 && !ThaumcraftApiHelper.isResearchComplete(player.username, research)) {
    		return false;
    	}
		
		ItemStack i2 = central.copy();
		if (recipeInput.getItemDamage()==OreDictionary.WILDCARD_VALUE) {
			i2.setItemDamage(OreDictionary.WILDCARD_VALUE);
		}
		
		if (!areItemStacksEqual(i2, recipeInput, true)) return false;
		
		ArrayList<ItemStack> ii = new ArrayList<ItemStack>();
		for (ItemStack is:input) {
			ii.add(is.copy());
		}
		
		for (ItemStack comp:components) {
			boolean b=false;
			for (int a=0;a<ii.size();a++) {
				 i2 = ii.get(a).copy();
				if (comp.getItemDamage()==OreDictionary.WILDCARD_VALUE) {
					i2.setItemDamage(OreDictionary.WILDCARD_VALUE);
				}
				if (areItemStacksEqual(i2, comp,true)) {
					ii.remove(a);
					b=true;
					break;
				}
			}
			if (!b) return false;
		}
//		System.out.println(ii.size());
		return ii.size()==0?true:false;
    }
	
	private boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, boolean fuzzy)
    {
		if (stack0==null && stack1!=null) return false;
		if (stack0!=null && stack1==null) return false;
		if (stack0==null && stack1==null) return true;
		boolean t1=false;
		if (fuzzy) {
			t1=true;
			int od = OreDictionary.getOreID(stack0);
			if (od!=-1) {
				ItemStack[] ores = OreDictionary.getOres(od).toArray(new ItemStack[]{});
				if (ThaumcraftApiHelper.containsMatch(false, new ItemStack[]{stack1}, ores))
					return true;
			}
		}
		else
			t1=ItemStack.areItemStackTagsEqual(stack0, stack1);		
        return stack0.itemID != stack1.itemID ? false : (stack0.getItemDamage() != stack1.getItemDamage() ? false : (stack0.stackSize > stack0.getMaxStackSize() ? false : t1));
    }
	
   
    public Object getRecipeOutput() {
		return recipeOutput;
    	
    }
    
    public AspectList getAspects() {
		return aspects;
    	
    }
    
    public String getResearch() {
		return research;
    	
    }

}
