/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.mail;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

public interface IPostOffice {

	void collectPostage(ItemStack[] stamps);

	IPostalState lodgeLetter(World world, ItemStack itemstack, boolean doLodge);

	ItemStack getAnyStamp(int max);

	ItemStack getAnyStamp(EnumPostage postage, int max);

	ItemStack getAnyStamp(EnumPostage[] postages, int max);

	void registerTradeStation(ITradeStation trade);

	void deregisterTradeStation(ITradeStation trade);

	Map<GameProfile, ITradeStation> getActiveTradeStations(World world);
}
