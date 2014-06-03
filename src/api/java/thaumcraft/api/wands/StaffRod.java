package thaumcraft.api.wands;

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
public class StaffRod extends WandRod {
	
	boolean runes=false;

	public StaffRod(String tag, int capacity, ItemStack item, int craftCost) {
		super(tag+"_staff", capacity, item, craftCost);
		this.texture = new ResourceLocation("thaumcraft","textures/models/wand_rod_"+tag+".png");
	}

	public StaffRod(String tag, int capacity, ItemStack item, int craftCost,
			IWandRodOnUpdate onUpdate, ResourceLocation texture) {
		super(tag+"_staff", capacity, item, craftCost, onUpdate, texture);
	}

	public StaffRod(String tag, int capacity, ItemStack item, int craftCost,
			IWandRodOnUpdate onUpdate) {
		super(tag+"_staff", capacity, item, craftCost, onUpdate);
		this.texture = new ResourceLocation("thaumcraft","textures/models/wand_rod_"+tag+".png");
	}

	public StaffRod(String tag, int capacity, ItemStack item, int craftCost,
			ResourceLocation texture) {
		super(tag+"_staff", capacity, item, craftCost, texture);
	}

	public boolean hasRunes() {
		return runes;
	}

	public void setRunes(boolean hasRunes) {
		this.runes = hasRunes;
	}

	
}
