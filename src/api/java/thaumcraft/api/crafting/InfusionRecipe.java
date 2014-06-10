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
	protected AspectList aspects;
	protected String research;
	private ItemStack[] components;
	private ItemStack recipeInput;
	protected Object recipeOutput;
	protected int instability;
	
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
		if (getRecipeInput()==null) return false;
			
		if (research.length()>0 && !ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), research)) {
    		return false;
    	}
		
		ItemStack i2 = central.copy();
		if (getRecipeInput().getItemDamage()==OreDictionary.WILDCARD_VALUE) {
			i2.setItemDamage(OreDictionary.WILDCARD_VALUE);
		}
		
		if (!areItemStacksEqual(i2, getRecipeInput(), true)) return false;
		
		ArrayList<ItemStack> ii = new ArrayList<ItemStack>();
		for (ItemStack is:input) {
			ii.add(is.copy());
		}
		
		for (ItemStack comp:getComponents()) {
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
		return ii.size()==0?true:false;
    }
	
	protected boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, boolean fuzzy)
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
        return stack0.getItem() != stack1.getItem() ? false : (stack0.getItemDamage() != stack1.getItemDamage() ? false : (stack0.stackSize > stack0.getMaxStackSize() ? false : t1));
    }
	
   
    public Object getRecipeOutput() {
		return getRecipeOutput(this.getRecipeInput());
    }
    
    public AspectList getAspects() {
		return getAspects(this.getRecipeInput());
    }

    public int getInstability() {
		return getInstability(this.getRecipeInput());
    }
    
    public String getResearch() {
		return research;
    }
    
	public ItemStack getRecipeInput() {
		return recipeInput;
	}

	public ItemStack[] getComponents() {
		return components;
	}
	
	public Object getRecipeOutput(ItemStack input) {
		return recipeOutput;
    }
    
    public AspectList getAspects(ItemStack input) {
		return aspects;
    }
    
    public int getInstability(ItemStack input) {
		return instability;
    }
}
