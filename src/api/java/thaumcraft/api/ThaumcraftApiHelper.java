package thaumcraft.api;

import java.lang.reflect.Method;
import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
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
			return s1.itemID == s2.itemID;
		} else
			return s1.itemID == s2.itemID && s1.getItemDamage() == s2.getItemDamage();
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
	            Class fake = Class.forName("thaumcraft.common.lib.ThaumcraftCraftingManager");
	            getObjectTags = fake.getMethod("getObjectTags", ItemStack.class);
	        }
	        ot = (AspectList) getObjectTags.invoke(null, is);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.ThaumcraftCraftingManager method getObjectTags");
	    }
		return ot;
	}

	public static AspectList getBonusObjectTags(ItemStack is,AspectList ot) {
		
	    try {
	        if(getBonusTags == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.ThaumcraftCraftingManager");
	            getBonusTags = fake.getMethod("getBonusTags", ItemStack.class, AspectList.class);
	        }
	        ot = (AspectList) getBonusTags.invoke(null, is, ot);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.ThaumcraftCraftingManager method getBonusTags");
	    }
		return ot;
	}

	public static AspectList generateTags(int id, int meta) {
	    try {
	        if(generateTags == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.ThaumcraftCraftingManager");
	            generateTags = fake.getMethod("generateTags", int.class, int.class);
	        }
	        return (AspectList) generateTags.invoke(null, id, meta);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.ThaumcraftCraftingManager method generateTags");
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
        return (target.itemID == input.itemID && ((target.getItemDamage() == OreDictionary.WILDCARD_VALUE && !strict) || target.getItemDamage() == input.getItemDamage()));
    }
    
    
    public static TileEntity getConnectableTile(World world, int x, int y, int z, ForgeDirection face) {
		TileEntity te = world.getBlockTileEntity(x+face.offsetX, y+face.offsetY, z+face.offsetZ);
		if (te instanceof IEssentiaTransport && ((IEssentiaTransport)te).isConnectable(face.getOpposite())) 
			return te;
		else
			return null;
	}
    
    public static TileEntity getConnectableTile(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		TileEntity te = world.getBlockTileEntity(x+face.offsetX, y+face.offsetY, z+face.offsetZ);
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
}
