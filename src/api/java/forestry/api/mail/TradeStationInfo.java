/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.mail;

import net.minecraft.item.ItemStack;

import com.mojang.authlib.GameProfile;

public class TradeStationInfo {
	public final GameProfile moniker;
	public final GameProfile owner;
	public final ItemStack tradegood;
	public final ItemStack[] required;
	public final IPostalState state;

	public TradeStationInfo(GameProfile moniker, GameProfile owner, ItemStack tradegood, ItemStack[] required, IPostalState state) {
		this.moniker = moniker;
		this.owner = owner;
		this.tradegood = tradegood;
		this.required = required;
		this.state = state;
	}
}
