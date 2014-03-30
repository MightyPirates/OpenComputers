package mcp.mobius.waila.api;

import net.minecraft.item.ItemStack;

public interface IWailaFMPDecorator {
	void decorateBlock(ItemStack itemStack, IWailaFMPAccessor accessor, IWailaConfigHandler config);	
}
