/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.gates;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public abstract class GateExpansionController {

	public final IGateExpansion type;
	public final TileEntity pipeTile;

	public GateExpansionController(IGateExpansion type, TileEntity pipeTile) {
		this.pipeTile = pipeTile;
		this.type = type;
	}

	public IGateExpansion getType() {
		return type;
	}

	public boolean isActive() {
		return false;
	}

	public void tick() {
	}

	public void startResolution() {
	}

	public boolean resolveAction(IAction action, int count) {
		return false;
	}

	public boolean isTriggerActive(ITrigger trigger, ITriggerParameter parameter) {
		return false;
	}

	public void addTriggers(List<ITrigger> list) {
	}

	public void addActions(List<IAction> list) {
	}

	public void writeToNBT(NBTTagCompound nbt) {
	}

	public void readFromNBT(NBTTagCompound nbt) {
	}
}
