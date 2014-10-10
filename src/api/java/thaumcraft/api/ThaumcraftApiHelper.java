package thaumcraft.api;

import java.lang.reflect.Method;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import cpw.mods.fml.common.FMLLog;

public class ThaumcraftApiHelper {
	
	public static AspectList cullTags(AspectList temp) {
		AspectList temp2 = new AspectList();
		for (Aspect tag:temp.getAspects()) {
			if (tag!=null)
				temp2.add(tag, temp.getAmount(tag));
		}
		while (temp2!=null && temp2.size()>10) {
			Aspect lowest = null;
			int low = Integer.MAX_VALUE;
			for (Aspect tag:temp2.getAspects()) {
				if (tag==null) continue;
				if (temp2.getAmount(tag)<low) {
					low = temp2.getAmount(tag);
					lowest = tag;
				}
			}
			temp2.aspects.remove(lowest);
		}
		return temp2; 
	}
	
	public static boolean areItemsEqual(ItemStack s1,ItemStack s2)
    {
		if (s1.isItemStackDamageable() && s2.isItemStackDamageable())
		{
			return s1.getItem() == s2.getItem();
		} else
			return s1.getItem() == s2.getItem() && s1.getItemDamage() == s2.getItemDamage();
    }

	static Method isResearchComplete;
	static Method getObjectTags;
	static Method getBonusTags;
	static Method generateTags;
	public static boolean isResearchComplete(String username, String researchkey) {
		boolean ot = false;
	    try {
	        if(isResearchComplete == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.research.ResearchManager");
	            isResearchComplete = fake.getMethod("isResearchComplete", String.class, String.class);
	        }
	        ot = (Boolean) isResearchComplete.invoke(null, username, researchkey);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.research.ResearchManager method isResearchComplete");
	    }
		return ot;
	}

	public static ItemStack getStackInRowAndColumn(Object instance, int row, int column) {
		ItemStack ot = null;
	    try {
	        Class fake = Class.forName("thaumcraft.common.tiles.TileMagicWorkbench");
	        Method getStackInRowAndColumn = fake.getMethod("getStackInRowAndColumn", int.class, int.class);
	        ot = (ItemStack) getStackInRowAndColumn.invoke(instance, row, column);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.tiles.TileMagicWorkbench method getStackInRowAndColumn");
	    }
		return ot;
	}

	public static AspectList getObjectAspects(ItemStack is) {
		AspectList ot = null;
	    try {
	        if(getObjectTags == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager");
	            getObjectTags = fake.getMethod("getObjectTags", ItemStack.class);
	        }
	        ot = (AspectList) getObjectTags.invoke(null, is);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.crafting.ThaumcraftCraftingManager method getObjectTags");
	    }
		return ot;
	}

	public static AspectList getBonusObjectTags(ItemStack is,AspectList ot) {
		
	    try {
	        if(getBonusTags == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager");
	            getBonusTags = fake.getMethod("getBonusTags", ItemStack.class, AspectList.class);
	        }
	        ot = (AspectList) getBonusTags.invoke(null, is, ot);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.crafting.ThaumcraftCraftingManager method getBonusTags");
	    }
		return ot;
	}

	public static AspectList generateTags(Item item, int meta) {
	    try {
	        if(generateTags == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager");
	            generateTags = fake.getMethod("generateTags", Item.class, int.class);
	        }
	        return (AspectList) generateTags.invoke(null, item, meta);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.crafting.ThaumcraftCraftingManager method generateTags");
	    }
		return null;
	}
	
	public static boolean containsMatch(boolean strict, ItemStack[] inputs, ItemStack... targets)
    {
        for (ItemStack input : inputs)
        {
            for (ItemStack target : targets)
            {
                if (itemMatches(target, input, strict))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean itemMatches(ItemStack target, ItemStack input, boolean strict)
    {
        if (input == null && target != null || input != null && target == null)
        {
            return false;
        }
        return (target.getItem() == input.getItem() && ((target.getItemDamage() == OreDictionary.WILDCARD_VALUE && !strict) || target.getItemDamage() == input.getItemDamage()));
    }
    
    
    public static TileEntity getConnectableTile(World world, int x, int y, int z, ForgeDirection face) {
		TileEntity te = world.getTileEntity(x+face.offsetX, y+face.offsetY, z+face.offsetZ);
		if (te instanceof IEssentiaTransport && ((IEssentiaTransport)te).isConnectable(face.getOpposite())) 
			return te;
		else
			return null;
	}
    
    public static TileEntity getConnectableTile(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		TileEntity te = world.getTileEntity(x+face.offsetX, y+face.offsetY, z+face.offsetZ);
		if (te instanceof IEssentiaTransport && ((IEssentiaTransport)te).isConnectable(face.getOpposite())) 
			return te;
		else
			return null;
	}
    
    private static HashMap<Integer, AspectList> allAspects= new HashMap<Integer, AspectList>();
    private static HashMap<Integer, AspectList> allCompoundAspects= new HashMap<Integer, AspectList>();
    
    public static AspectList getAllAspects(int amount) {
    	if (allAspects.get(amount)==null) {
    		AspectList al = new AspectList();
    		for (Aspect aspect:Aspect.aspects.values()) {
    			al.add(aspect, amount);
    		}
    		allAspects.put(amount, al);
    	} 
    	return allAspects.get(amount);
    }
    
    public static AspectList getAllCompoundAspects(int amount) {
    	if (allCompoundAspects.get(amount)==null) {
    		AspectList al = new AspectList();
    		for (Aspect aspect:Aspect.getCompoundAspects()) {
    			al.add(aspect, amount);
    		}
    		allCompoundAspects.put(amount, al);
    	} 
    	return allCompoundAspects.get(amount);
    }
    
    static Method consumeVisFromWand;
	/**
	 * Use to subtract vis from a wand for most operations
	 * Wands store vis differently so "real" vis costs need to be multiplied by 100 before calling this method
	 * @param wand the wand itemstack
	 * @param player the player using the wand
	 * @param cost the cost of the operation. 
	 * @param doit actually subtract the vis from the wand if true - if false just simulate the result
	 * @param crafting is this a crafting operation or not - if 
	 * false then things like frugal and potency will apply to the costs
	 * @return was the vis successfully subtracted
	 */
	public static boolean consumeVisFromWand(ItemStack wand, EntityPlayer player, 
			AspectList cost, boolean doit, boolean crafting) {
		boolean ot = false;
	    try {
	        if(consumeVisFromWand == null) {
	            Class fake = Class.forName("thaumcraft.common.items.wands.ItemWandCasting");
	            consumeVisFromWand = fake.getMethod("consumeAllVis", 
	            		ItemStack.class, EntityPlayer.class, AspectList.class, boolean.class, boolean.class);
	        }
	        ot = (Boolean) consumeVisFromWand.invoke(
	        		consumeVisFromWand.getDeclaringClass().cast(wand.getItem()), wand, player, cost, doit, crafting);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.items.wands.ItemWandCasting method consumeAllVis");
	    }
		return ot;
	}
	
	static Method consumeVisFromWandCrafting;
	/**
	 * Subtract vis for use by a crafting mechanic. Costs are calculated slightly 
	 * differently and things like the frugal enchant is ignored
	 * Must NOT be multiplied by 100 - send the actual vis cost
	 * @param wand the wand itemstack
	 * @param player the player using the wand
	 * @param cost the cost of the operation. 
	 * @param doit actually subtract the vis from the wand if true - if false just simulate the result
	 * @return was the vis successfully subtracted
	 */
	public static boolean consumeVisFromWandCrafting(ItemStack wand, EntityPlayer player, 
			AspectList cost, boolean doit) {
		boolean ot = false;
	    try {
	        if(consumeVisFromWandCrafting == null) {
	            Class fake = Class.forName("thaumcraft.common.items.wands.ItemWandCasting");
	            consumeVisFromWandCrafting = fake.getMethod("consumeAllVisCrafting", 
	            		ItemStack.class, EntityPlayer.class, AspectList.class, boolean.class);
	        }
	        ot = (Boolean) consumeVisFromWandCrafting.invoke(
	        		consumeVisFromWandCrafting.getDeclaringClass().cast(wand.getItem()), wand, player, cost, doit);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.items.wands.ItemWandCasting method consumeAllVisCrafting");
	    }
		return ot;
	}
	
	static Method consumeVisFromInventory;
	/**
	 * Subtract vis from a wand the player is carrying. Works like consumeVisFromWand in that actual vis
	 * costs should be multiplied by 100. The costs are handled like crafting however and things like 
	 * frugal don't effect them
	 * @param player the player using the wand
	 * @param cost the cost of the operation. 
	 * @return was the vis successfully subtracted
	 */
	public static boolean consumeVisFromInventory(EntityPlayer player, AspectList cost) {
		boolean ot = false;
	    try {
	        if(consumeVisFromInventory == null) {
	            Class fake = Class.forName("thaumcraft.common.items.wands.WandManager");
	            consumeVisFromInventory = fake.getMethod("consumeVisFromInventory", 
	            		EntityPlayer.class, AspectList.class);
	        }
	        ot = (Boolean) consumeVisFromInventory.invoke(null, player, cost);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.items.wands.WandManager method consumeVisFromInventory");
	    }
		return ot;
	}
}
