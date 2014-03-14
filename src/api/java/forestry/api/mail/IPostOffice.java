package forestry.api.mail;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IPostOffice {

	void collectPostage(ItemStack[] stamps);

	IPostalState lodgeLetter(World world, ItemStack itemstack, boolean doLodge);

	ItemStack getAnyStamp(int max);

	ItemStack getAnyStamp(EnumPostage postage, int max);

	ItemStack getAnyStamp(EnumPostage[] postages, int max);

	void registerTradeStation(ITradeStation trade);

	void deregisterTradeStation(ITradeStation trade);

	Map<String, ITradeStation> getActiveTradeStations(World world);
}
