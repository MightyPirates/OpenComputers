package forestry.api.mail;

import net.minecraft.item.ItemStack;

public class TradeStationInfo {
	public final String moniker;
	public final String owner;
	public final ItemStack tradegood;
	public final ItemStack[] required;
	public final IPostalState state;

	public TradeStationInfo(String moniker, String owner, ItemStack tradegood, ItemStack[] required, IPostalState state) {
		this.moniker = moniker;
		this.owner = owner;
		this.tradegood = tradegood;
		this.required = required;
		this.state = state;
	}
}
