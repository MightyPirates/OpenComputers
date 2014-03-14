package forestry.api.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.Cancelable;

/**
 * Use @ForgeSubscribe on a method taking this event as an argument. Will fire whenever a backpack tries to store an item. Processing will stop if the stacksize
 * of stackToStow drops to 0 or less or the event is canceled.
 */
@Cancelable
public class BackpackStowEvent extends BackpackEvent {

	public final ItemStack stackToStow;

	public BackpackStowEvent(EntityPlayer player, IBackpackDefinition backpackDefinition, IInventory backpackInventory, ItemStack stackToStow) {
		super(player, backpackDefinition, backpackInventory);
		this.stackToStow = stackToStow;
	}
}
