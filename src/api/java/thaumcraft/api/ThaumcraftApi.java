package thaumcraft.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;


/**
 * @author Azanor
 *
 *
 * IMPORTANT: If you are adding your own aspects to items it is a good idea to do it AFTER Thaumcraft adds its aspects, otherwise odd things may happen.
 *
 */
public class ThaumcraftApi {
	
	//Materials	
	public static ToolMaterial toolMatThaumium = EnumHelper.addToolMaterial("THAUMIUM", 3, 400, 7F, 2, 22);
	public static ToolMaterial toolMatElemental = EnumHelper.addToolMaterial("THAUMIUM_ELEMENTAL", 3, 1500, 10F, 3, 18);
	public static ArmorMaterial armorMatThaumium = EnumHelper.addArmorMaterial("THAUMIUM", 25, new int[] { 2, 6, 5, 2 }, 25);
	public static ArmorMaterial armorMatSpecial = EnumHelper.addArmorMaterial("SPECIAL", 25, new int[] { 1, 3, 2, 1 }, 25);
	
	//Enchantment references
	public static int enchantFrugal;
	public static int enchantPotency;
	public static int enchantWandFortune;
	public static int enchantHaste;
	public static int enchantRepair;
	
	//Miscellaneous
	/**
	 * Portable Hole Block-id Blacklist. 
	 * Simply add the block-id's of blocks you don't want the portable hole to go through.
	 */
	public static ArrayList<Block> portableHoleBlackList = new ArrayList<Block>();
	
	
	//RESEARCH/////////////////////////////////////////
	public static ArrayList<IScanEventHandler> scanEventhandlers = new ArrayList<IScanEventHandler>();
	public static ArrayList<EntityTags> scanEntities = new ArrayList<EntityTags>();
	public static class EntityTagsNBT {
		public EntityTagsNBT(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		public String name;
		public Object value;
	}
	public static class EntityTags {
		public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
			this.entityName = entityName;
			this.nbts = nbts;
			this.aspects = aspects;
		}
		public String entityName;
		public EntityTagsNBT[] nbts;
		public AspectList aspects;
	}
	
	/**
	 * not really working atm, so ignore it for now
	 * @param scanEventHandler
	 */
	public static void registerScanEventhandler(IScanEventHandler scanEventHandler) {
		scanEventhandlers.add(scanEventHandler);
	}
	
	/**
	 * This is used to add aspects to entities which you can then scan using a thaumometer.
	 * Also used to calculate vis drops from mobs.
	 * @param entityName
	 * @param aspects
	 * @param nbt you can specify certain nbt keys and their values 
	 * 			  to differentiate between mobs. <br>For example the normal and wither skeleton:
	 * 	<br>ThaumcraftApi.registerEntityTag("Skeleton", (new AspectList()).add(Aspect.DEATH, 5));
	 * 	<br>ThaumcraftApi.registerEntityTag("Skeleton", (new AspectList()).add(Aspect.DEATH, 8), new NBTTagByte("SkeletonType",(byte) 1));
	 */
	public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt ) {
		scanEntities.add(new EntityTags(entityName,aspects,nbt));
	}
	
	//RECIPES/////////////////////////////////////////
	private static ArrayList craftingRecipes = new ArrayList();	
	private static HashMap<Object,ItemStack> smeltingBonus = new HashMap<Object,ItemStack>();
	
	/**
	 * This method is used to determine what bonus items are generated when the infernal furnace smelts items
	 * @param in The input of the smelting operation. e.g. new ItemStack(Block.oreGold)
	 * @param out The bonus item that can be produced from the smelting operation e.g. new ItemStack(nuggetGold,0,0).
	 * Stacksize should be 0 unless you want to guarantee that at least 1 item is always produced.
	 */
	public static void addSmeltingBonus(ItemStack in, ItemStack out) {
		smeltingBonus.put(
				Arrays.asList(in.getItem(),in.getItemDamage()), 
				new ItemStack(out.getItem(),0,out.getItemDamage()));
	}
	
	/**
	 * This method is used to determine what bonus items are generated when the infernal furnace smelts items
	 * @param in The ore dictionary input of the smelting operation. e.g. "oreGold"
	 * @param out The bonus item that can be produced from the smelting operation e.g. new ItemStack(nuggetGold,0,0).
	 * Stacksize should be 0 unless you want to guarantee that at least 1 item is always produced.
	 */
	public static void addSmeltingBonus(String in, ItemStack out) {
		smeltingBonus.put(	in, new ItemStack(out.getItem(),0,out.getItemDamage()));
	}
	
	/**
	 * Returns the bonus item produced from a smelting operation in the infernal furnace
	 * @param in The input of the smelting operation. e.g. new ItemStack(oreGold)
	 * @return the The bonus item that can be produced
	 */
	public static ItemStack getSmeltingBonus(ItemStack in) {
		ItemStack out = smeltingBonus.get(Arrays.asList(in.getItem(),in.getItemDamage()));
		if (out==null) {
			out = smeltingBonus.get(Arrays.asList(in.getItem(),OreDictionary.WILDCARD_VALUE));
		}
		if (out==null) {
			String od = OreDictionary.getOreName( OreDictionary.getOreID(in));
			out = smeltingBonus.get(od);
		}
		return out;
	}
	
	public static List getCraftingRecipes() {
		return craftingRecipes;
	}
	
	/**
	 * @param research the research key required for this recipe to work. Leave blank if it will work without research
	 * @param result the recipe output
	 * @param aspects the vis cost per aspect. 
	 * @param recipe The recipe. Format is exactly the same as vanilla recipes. Input itemstacks are NBT sensitive.
	 */
	public static ShapedArcaneRecipe addArcaneCraftingRecipe(String research, ItemStack result, AspectList aspects, Object ... recipe)
    {
		ShapedArcaneRecipe r= new ShapedArcaneRecipe(research, result, aspects, recipe);
        craftingRecipes.add(r);
		return r;
    }
	
	/**
	 * @param research the research key required for this recipe to work. Leave blank if it will work without research
	 * @param result the recipe output
	 * @param aspects the vis cost per aspect
	 * @param recipe The recipe. Format is exactly the same as vanilla shapeless recipes. Input itemstacks are NBT sensitive.
	 */
	public static ShapelessArcaneRecipe addShapelessArcaneCraftingRecipe(String research, ItemStack result, AspectList aspects, Object ... recipe)
    {
		ShapelessArcaneRecipe r = new ShapelessArcaneRecipe(research, result, aspects, recipe);
        craftingRecipes.add(r);
		return r;
    }
	
	/**
	 * @param research the research key required for this recipe to work. Leave blank if it will work without research
	 * @param result the recipe output. It can either be an itemstack or an nbt compound tag that will be added to the central item
	 * @param instability a number that represents the N in 1000 chance for the infusion altar to spawn an
	 * 		  instability effect each second while the crafting is in progress
	 * @param aspects the essentia cost per aspect. 
	 * @param aspects input the central item to be infused
	 * @param recipe An array of items required to craft this. Input itemstacks are NBT sensitive. 
	 * 				Infusion crafting components are automatically "fuzzy" and the oredict will be checked for possible matches.
	 * 
	 */
	public static InfusionRecipe addInfusionCraftingRecipe(String research, Object result, int instability, AspectList aspects, ItemStack input,ItemStack[] recipe)
    {
		if (!(result instanceof ItemStack || result instanceof Object[])) return null;
		InfusionRecipe r= new InfusionRecipe(research, result, instability, aspects, input, recipe);
        craftingRecipes.add(r);
		return r;
    }
	
	/**
	 * @param research the research key required for this recipe to work. Leave blank if it will work without research
	 * @param enchantment the enchantment that will be applied to the item
	 * @param instability a number that represents the N in 1000 chance for the infusion altar to spawn an
	 * 		  instability effect each second while the crafting is in progress
	 * @param aspects the essentia cost per aspect. 
	 * @param recipe An array of items required to craft this. Input itemstacks are NBT sensitive. 
	 * 				Infusion crafting components are automatically "fuzzy" and the oredict will be checked for possible matches.
	 * 
	 */
	public static InfusionEnchantmentRecipe addInfusionEnchantmentRecipe(String research, Enchantment enchantment, int instability, AspectList aspects, ItemStack[] recipe)
    {
		InfusionEnchantmentRecipe r= new InfusionEnchantmentRecipe(research, enchantment, instability, aspects, recipe);
        craftingRecipes.add(r);
		return r;
    }
	
	/**
	 * @param stack the recipe result
	 * @return the recipe
	 */
	public static InfusionRecipe getInfusionRecipe(ItemStack res) {
		for (Object r:getCraftingRecipes()) {
			if (r instanceof InfusionRecipe) {
				if (((InfusionRecipe)r).getRecipeOutput() instanceof ItemStack) {
					if (((ItemStack) ((InfusionRecipe)r).getRecipeOutput()).isItemEqual(res))
						return (InfusionRecipe)r;
				} 
			}
		}
		return null;
	}

    
    /**
     * @param key the research key required for this recipe to work. 
     * @param result the output result
     * @param catalyst an itemstack of the catalyst or a string if it is an ore dictionary item
     * @param cost the vis cost
     * @param tags the aspects required to craft this
     */
    public static CrucibleRecipe addCrucibleRecipe(String key, ItemStack result, Object catalyst, AspectList tags) {
    	CrucibleRecipe rc = new CrucibleRecipe(key, result, catalyst, tags);
    	getCraftingRecipes().add(rc);
		return rc;
	}
    
	
	/**
	 * @param stack the recipe result
	 * @return the recipe
	 */
	public static CrucibleRecipe getCrucibleRecipe(ItemStack stack) {
		for (Object r:getCraftingRecipes()) {
			if (r instanceof CrucibleRecipe) {
				if (((CrucibleRecipe)r).getRecipeOutput().isItemEqual(stack))
					return (CrucibleRecipe)r;
			}
		}
		return null;
	}
	
	/**
	 * Used by the thaumonomicon drilldown feature.
	 * @param stack the item
	 * @return the thaumcraft recipe key that produces that item. 
	 */
	private static HashMap<int[],Object[]> keyCache = new HashMap<int[],Object[]>();
	
	public static Object[] getCraftingRecipeKey(EntityPlayer player, ItemStack stack) {
		int[] key = new int[] {Item.getIdFromItem(stack.getItem()),stack.getItemDamage()};
		if (keyCache.containsKey(key)) {
			if (keyCache.get(key)==null) return null;
			if (ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), (String)(keyCache.get(key))[0]))
				return keyCache.get(key);
			else 
				return null;
		}
		for (ResearchCategoryList rcl:ResearchCategories.researchCategories.values()) {
			for (ResearchItem ri:rcl.research.values()) {
				if (ri.getPages()==null) continue;
				for (int a=0;a<ri.getPages().length;a++) {
					ResearchPage page = ri.getPages()[a];
					if (page.recipeOutput!=null && stack !=null && page.recipeOutput.isItemEqual(stack)) {
						keyCache.put(key,new Object[] {ri.key,a});
						if (ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), ri.key))
							return new Object[] {ri.key,a};
						else 
							return null;
					}
				}
			}
		}
		keyCache.put(key,null);
		return null;
	}
	
	//ASPECTS////////////////////////////////////////
	
	public static ConcurrentHashMap<List,AspectList> objectTags = new ConcurrentHashMap<List,AspectList>();
	
	/**
	 * Checks to see if the passed item/block already has aspects associated with it.
	 * @param id
	 * @param meta
	 * @return 
	 */
	public static boolean exists(Item item, int meta) {
		AspectList tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item,meta));
		if (tmp==null) {
			tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item,OreDictionary.WILDCARD_VALUE));
			if (meta==OreDictionary.WILDCARD_VALUE && tmp==null) {
				int index=0;
				do {
					tmp = ThaumcraftApi.objectTags.get(Arrays.asList(item,index));
					index++;
				} while (index<16 && tmp==null);
			}
			if (tmp==null) return false;
		}
		
		return true;
	}
	
	/**
	 * Used to assign apsects to the given item/block. Here is an example of the declaration for cobblestone:<p>
	 * <i>ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.cobblestone), (new AspectList()).add(Aspect.ENTROPY, 1).add(Aspect.EARTH, 1));</i>
	 * @param item the item passed. Pass OreDictionary.WILDCARD_VALUE if all damage values of this item/block should have the same aspects
	 * @param aspects A ObjectTags object of the associated aspects
	 */
	public static void registerObjectTag(ItemStack item, AspectList aspects) {
		if (aspects==null) aspects=new AspectList();
		try {
		objectTags.put(Arrays.asList(item.getItem(),item.getItemDamage()), aspects);
		} catch (Exception e) {}
	}	
	
	
	/**
	 * Used to assign apsects to the given item/block. Here is an example of the declaration for cobblestone:<p>
	 * <i>ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.cobblestone), new int[]{0,1}, (new AspectList()).add(Aspect.ENTROPY, 1).add(Aspect.EARTH, 1));</i>
	 * @param item
	 * @param meta A range of meta values if you wish to lump several item meta's together as being the "same" item (i.e. stair orientations)
	 * @param aspects A ObjectTags object of the associated aspects
	 */
	public static void registerObjectTag(ItemStack item, int[] meta, AspectList aspects) {
		if (aspects==null) aspects=new AspectList();
		try {
		objectTags.put(Arrays.asList(item.getItem(),meta), aspects);
		} catch (Exception e) {}
	}
	
	/**
	 * Used to assign apsects to the given ore dictionary item. 
	 * @param oreDict the ore dictionary name
	 * @param aspects A ObjectTags object of the associated aspects
	 */
	public static void registerObjectTag(String oreDict, AspectList aspects) {
		if (aspects==null) aspects=new AspectList();
		ArrayList<ItemStack> ores = OreDictionary.getOres(oreDict);
		if (ores!=null && ores.size()>0) {
			for (ItemStack ore:ores) {
				try {
				objectTags.put(Arrays.asList(ore.getItem(), ore.getItemDamage()), aspects);
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Used to assign aspects to the given item/block. 
	 * Attempts to automatically generate aspect tags by checking registered recipes.
	 * Here is an example of the declaration for pistons:<p>
	 * <i>ThaumcraftApi.registerComplexObjectTag(new ItemStack(Blocks.cobblestone), (new AspectList()).add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 4));</i>
	 * @param item, pass OreDictionary.WILDCARD_VALUE to meta if all damage values of this item/block should have the same aspects
	 * @param aspects A ObjectTags object of the associated aspects
	 */
	public static void registerComplexObjectTag(ItemStack item, AspectList aspects ) {
		if (!exists(item.getItem(),item.getItemDamage())) {
			AspectList tmp = ThaumcraftApiHelper.generateTags(item.getItem(), item.getItemDamage());
			if (tmp != null && tmp.size()>0) {
				for(Aspect tag:tmp.getAspects()) {
					aspects.add(tag, tmp.getAmount(tag));
				}
			}
			registerObjectTag(item,aspects);
		} else {
			AspectList tmp = ThaumcraftApiHelper.getObjectAspects(item);
			for(Aspect tag:aspects.getAspects()) {
				tmp.merge(tag, tmp.getAmount(tag));
			}
			registerObjectTag(item,tmp);
		}
	}
		
	//CROPS //////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * To define mod crops you need to use FMLInterModComms in your @Mod.Init method.
	 * There are two 'types' of crops you can add. Standard crops and clickable crops.
	 * 
	 * Standard crops work like normal vanilla crops - they grow until a certain metadata 
	 * value is reached and you harvest them by destroying the block and collecting the blocks.
	 * You need to create and ItemStack that tells the golem what block id and metadata represents
	 * the crop when fully grown. Sending a metadata of [OreDictionary.WILDCARD_VALUE] will mean the metadata won't get 
	 * checked.
	 * Example for vanilla wheat: 
	 * FMLInterModComms.sendMessage("Thaumcraft", "harvestStandardCrop", new ItemStack(Block.crops,1,7));
	 *  
	 * Clickable crops are crops that you right click to gather their bounty instead of destroying them.
	 * As for standard crops, you need to create and ItemStack that tells the golem what block id 
	 * and metadata represents the crop when fully grown. The golem will trigger the blocks onBlockActivated method. 
	 * Sending a metadata of [OreDictionary.WILDCARD_VALUE] will mean the metadata won't get checked.
	 * Example (this will technically do nothing since clicking wheat does nothing, but you get the idea): 
	 * FMLInterModComms.sendMessage("Thaumcraft", "harvestClickableCrop", new ItemStack(Block.crops,1,7));
	 * 
	 * Stacked crops (like reeds) are crops that you wish the bottom block should remain after harvesting.
	 * As for standard crops, you need to create and ItemStack that tells the golem what block id 
	 * and metadata represents the crop when fully grown. Sending a metadata of [OreDictionary.WILDCARD_VALUE] will mean the actualy md won't get 
	 * checked. If it has the order upgrade it will only harvest if the crop is more than one block high.
	 * Example: 
	 * FMLInterModComms.sendMessage("Thaumcraft", "harvestStackedCrop", new ItemStack(Block.reed,1,7));
	 */
	
	//NATIVE CLUSTERS //////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * You can define certain ores that will have a chance to produce native clusters via FMLInterModComms 
	 * in your @Mod.Init method using the "nativeCluster" string message.
	 * The format should be: 
	 * "[ore item/block id],[ore item/block metadata],[cluster item/block id],[cluster item/block metadata],[chance modifier float]"
	 * 
	 * NOTE: The chance modifier is a multiplier applied to the default chance for that cluster to be produced (default 27.5% for a pickaxe of the core)
	 * 
	 * Example for vanilla iron ore to produce one of my own native iron clusters (assuming default id's) at double the default chance: 
	 * FMLInterModComms.sendMessage("Thaumcraft", "nativeCluster","15,0,25016,16,2.0");
	 */
	
	//LAMP OF GROWTH BLACKLIST ///////////////////////////////////////////////////////////////////////////
	/**
	 * You can blacklist crops that should not be effected by the Lamp of Growth via FMLInterModComms 
	 * in your @Mod.Init method using the "lampBlacklist" itemstack message.
	 * Sending a metadata of [OreDictionary.WILDCARD_VALUE] will mean the metadata won't get checked.
	 * Example for vanilla wheat: 
	 * FMLInterModComms.sendMessage("Thaumcraft", "lampBlacklist", new ItemStack(Block.crops,1,OreDictionary.WILDCARD_VALUE));
	 */
	
	//DIMENSION BLACKLIST ///////////////////////////////////////////////////////////////////////////
	/**
	 * You can blacklist a dimension to not spawn certain thaumcraft features 
	 * in your @Mod.Init method using the "dimensionBlacklist" string message in the format "[dimension]:[level]"
	 * The level values are as follows:
	 * [0] stop all tc spawning and generation
	 * [1] allow ore and node generation
	 * [2] allow mob spawning
	 * [3] allow ore and node gen + mob spawning
	 * Example: 
	 * FMLInterModComms.sendMessage("Thaumcraft", "dimensionBlacklist", "15:1");
	 */
	
	//BIOME BLACKLIST ///////////////////////////////////////////////////////////////////////////
	/**
	 * You can blacklist a biome to not spawn certain thaumcraft features 
	 * in your @Mod.Init method using the "biomeBlacklist" string message in the format "[biome id]:[level]"
	 * The level values are as follows:
	 * [0] stop all tc spawning and generation
	 * [1] allow ore and node generation
	 * [2] allow mob spawning
	 * [3] allow ore and node gen + mob spawning
	 * Example: 
	 * FMLInterModComms.sendMessage("Thaumcraft", "biomeBlacklist", "180:2");
	 */
}
