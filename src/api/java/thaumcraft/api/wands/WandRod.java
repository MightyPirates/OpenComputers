package thaumcraft.api.wands;

import java.util.LinkedHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * @author Azanor
 * 
 * This class is used to keep the material information for the various rods. 
 * It is also used to generate the wand recipes ingame.
 *
 */
public class WandRod {

	
	private String tag;
	
	/**
	 * Cost to craft this wand. Combined with the rod cost.
	 */
	private int craftCost;
	
	/** 
	 * The amount of vis that can be stored - this number is actually multiplied 
	 * by 100 for use by the wands internals
	 */
	int capacity;   

	/**
	 * The texture that will be used for the ingame wand rod
	 */
	protected ResourceLocation texture;
	
	/**
	 * the actual item that makes up this rod and will be used to generate the wand recipes
	 */
	ItemStack item;
	
	/**
	 * A class that will be called whenever the wand onUpdate tick is run
	 */
	IWandRodOnUpdate onUpdate;
	
	/**
	 * Does the rod glow in the dark?
	 */
	boolean glow;

	public static LinkedHashMap<String,WandRod> rods = new LinkedHashMap<String,WandRod>();
	
	public WandRod (String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
		this.setTag(tag);
		this.capacity = capacity;
		this.texture = texture;
		this.item=item;
		this.setCraftCost(craftCost);
		rods.put(tag, this);
	}
	
	public WandRod (String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
		this.setTag(tag);
		this.capacity = capacity;
		this.texture = texture;
		this.item=item;
		this.setCraftCost(craftCost);
		rods.put(tag, this);
		this.onUpdate = onUpdate;
	}

	public WandRod (String tag, int capacity, ItemStack item, int craftCost) {
		this.setTag(tag);
		this.capacity = capacity;
		this.texture = new ResourceLocation("thaumcraft","textures/models/wand_rod_"+getTag()+".png");
		this.item=item;
		this.setCraftCost(craftCost);
		rods.put(tag, this);
	}
	
	public WandRod (String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
		this.setTag(tag);
		this.capacity = capacity;
		this.texture = new ResourceLocation("thaumcraft","textures/models/wand_rod_"+getTag()+".png");
		this.item=item;
		this.setCraftCost(craftCost);
		rods.put(tag, this);
		this.onUpdate = onUpdate;
	}
	
	public String getTag() {
		return tag;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public ResourceLocation getTexture() {
		return texture;
	}

	public void setTexture(ResourceLocation texture) {
		this.texture = texture;
	}

	public ItemStack getItem() {
		return item;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public int getCraftCost() {
		return craftCost;
	}

	public void setCraftCost(int craftCost) {
		this.craftCost = craftCost;
	}

	public IWandRodOnUpdate getOnUpdate() {
		return onUpdate;
	}

	public void setOnUpdate(IWandRodOnUpdate onUpdate) {
		this.onUpdate = onUpdate;
	}

	public boolean isGlowing() {
		return glow;
	}

	public void setGlowing(boolean hasGlow) {
		this.glow = hasGlow;
	}

	//  Some examples:
	//	WandRod WAND_ROD_WOOD = new WandRod("wood",25,new ItemStack(Item.stick),1);
	//	WandRod WAND_ROD_BLAZE = new WandRod("blaze",100,new ItemStack(Item.blazeRod),7,new WandRodBlazeOnUpdate());
}
