package mcp.mobius.waila.api;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface IWailaBlockDecorator {

	void decorateBlock(ItemStack itemStack, IWailaDataAccessor accessor, IWailaConfigHandler config);	
	
}
