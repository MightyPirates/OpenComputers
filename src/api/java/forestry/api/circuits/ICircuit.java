/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.circuits;

import java.util.List;

import net.minecraft.tileentity.TileEntity;

public interface ICircuit {
	String getUID();

	boolean requiresDiscovery();

	int getLimit();

	String getName();

	boolean isCircuitable(TileEntity tile);

	void onInsertion(int slot, TileEntity tile);

	void onLoad(int slot, TileEntity tile);

	void onRemoval(int slot, TileEntity tile);

	void onTick(int slot, TileEntity tile);

	void addTooltip(List<String> list);
}
