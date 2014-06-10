package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;

public class InfusionEnchantmentRecipe
{
	
	public AspectList aspects;
	public String research;
	public ItemStack[] components;
	public Enchantment enchantment;
	public int recipeXP;
	public int instability;
	
	public InfusionEnchantmentRecipe(String research, Enchantment input, int inst, 
			AspectList aspects2, ItemStack[] recipe) {
		this.research = research;
		this.enchantment = input;
		this.aspects = aspects2;
		this.components = recipe;
		this.instability = inst;
		this.recipeXP = Math.max(1, input.getMinEnchantability(1)/3);
	}

	/**
     * Used to check if a recipe matches current crafting inventory
     * @param player 
     */
	public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
		if (research.length()>0 && !ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), research)) {
    		return false;
    	}
		
		if (!enchantment.canApply(central) || !central.getItem().isItemTool(central)) {
			return false;
		}
				
		Map map1 = EnchantmentHelper.getEnchantments(central);
		Iterator iterator = map1.keySet().iterator();
        while (iterator.hasNext())
        {
        	int j1 = ((Integer)iterator.next()).intValue();
            Enchantment ench = Enchantment.enchantmentsList[j1];
            if (j1 == enchantment.effectId &&
            		EnchantmentHelper.getEnchantmentLevel(j1, central)>=ench.getMaxLevel())
            	return false;
            if (enchantment.effectId != ench.effectId && 
            	(!enchantment.canApplyTogether(ench) ||
            	!ench.canApplyTogether(enchantment))) {
            	return false;
            }
        }
		
		ItemStack i2 = null;
		
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
        return stack0.getItem() != stack1.getItem() ? false : (stack0.getItemDamage() != stack1.getItemDamage() ? false : (stack0.stackSize > stack0.getMaxStackSize() ? false : t1));
    }
	
   
    public Enchantment getEnchantment() {
		return enchantment;
    	
    }
    
    public AspectList getAspects() {
		return aspects;
    	
    }
    
    public String getResearch() {
		return research;
    	
    }

	public int calcInstability(ItemStack recipeInput) {
		int i = 0;
		Map map1 = EnchantmentHelper.getEnchantments(recipeInput);
		Iterator iterator = map1.keySet().iterator();
        while (iterator.hasNext())
        {
        	int j1 = ((Integer)iterator.next()).intValue();
        	i += EnchantmentHelper.getEnchantmentLevel(j1, recipeInput);
        }
		return (i/2) + instability;
	}

	public int calcXP(ItemStack recipeInput) {
		return recipeXP * (1+EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, recipeInput));
	}

	public float getEssentiaMod(ItemStack recipeInput) {
		float mod = EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, recipeInput);
		Map map1 = EnchantmentHelper.getEnchantments(recipeInput);
		Iterator iterator = map1.keySet().iterator();
        while (iterator.hasNext())
        {
        	int j1 = ((Integer)iterator.next()).intValue();
        	if (j1 != enchantment.effectId)
        		mod += EnchantmentHelper.getEnchantmentLevel(j1, recipeInput) * .1f;
        }
		return mod;
	}

}
