package forestry.api.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.event.Cancelable;

/**
 * Use @ForgeSubscribe on a method taking this event as an argument. Will fire whenever a backpack tries to resupply to a player inventory. Processing will stop
 * if the event is canceled.
 */
@Cancelable
public class BackpackResupplyEvent extends BackpackEvent {

	public BackpackResupplyEvent(EntityPlayer player, IBackpackDefinition backpackDefinition, IInventory backpackInventory) {
		super(player, backpackDefinition, backpackInventory);
	}

}
