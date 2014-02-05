package ic2.api.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Wrapper for inserting interfaces into items you don't own.
 * 
 * @author Richard
 */
public class ItemWrapper {
	private static final Multimap<Item, IBoxable> boxableItems = ArrayListMultimap.create();
	private static final Multimap<Item, IMetalArmor> metalArmorItems = ArrayListMultimap.create();
	
	public static void registerBoxable(Item item, IBoxable boxable) {
		boxableItems.put(item, boxable);
	}
	
	public static boolean canBeStoredInToolbox(ItemStack stack) {
		Item item = stack.getItem();
		// use customs first to allow for overriding behavior
		for (IBoxable boxable : boxableItems.get(item)) {
			if (boxable.canBeStoredInToolbox(stack)) return true;
		}
		
		if (item instanceof IBoxable && ((IBoxable) item).canBeStoredInToolbox(stack)) return true;
		
		return false;
	}
	
	public static void registerMetalArmor(Item item, IMetalArmor armor) {
		metalArmorItems.put(item, armor);
	}
	
	public static boolean isMetalArmor(ItemStack stack, EntityPlayer player) {
		Item item = stack.getItem();
		// use customs first to allow for overriding behavior
		for (IMetalArmor metalArmor : metalArmorItems.get(item)) {
			if (metalArmor.isMetalArmor(stack, player)) return true;
		}
		
		if (item instanceof IMetalArmor && ((IMetalArmor) item).isMetalArmor(stack, player)) return true;
		
		return false;
	}
}
