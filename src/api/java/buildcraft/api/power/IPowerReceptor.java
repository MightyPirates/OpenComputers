/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.power;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * This interface should be implemented by any Tile Entity that wishes to be
 * able to receive power.
 */
public interface IPowerReceptor {

	/**
	 * Get the PowerReceiver for this side of the block. You can return the same
	 * PowerReceiver for all sides or one for each side.
	 *
	 * You should NOT return null to this method unless you mean to NEVER
	 * receive power from that side. Returning null, after previous returning a
	 * PowerReceiver, will most likely cause pipe connections to derp out and
	 * engines to eventually explode.
	 *
	 * @param side
	 * @return
	 */
	public PowerHandler.PowerReceiver getPowerReceiver(ForgeDirection side);

	/**
	 * Call back from the PowerHandler that is called when the stored power
	 * exceeds the activation power.
	 *
	 * It can be triggered by update() calls or power modification calls.
	 *
	 * @param workProvider
	 */
	public void doWork(PowerHandler workProvider);

	public World getWorld();
}
