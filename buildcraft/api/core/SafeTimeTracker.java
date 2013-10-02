/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.core;

import net.minecraft.world.World;

public class SafeTimeTracker
{

	private long lastMark = Long.MIN_VALUE;
	private long duration = -1;

	/**
	 * Return true if a given delay has passed since last time marked was called successfully.
	 */
	public boolean markTimeIfDelay(World world, long delay)
	{
		if (world == null)
			return false;

		long currentTime = world.getTotalWorldTime();

		if (currentTime < lastMark)
		{
			lastMark = currentTime;
			return false;
		}
		else if (lastMark + delay <= currentTime)
		{
			duration = currentTime - lastMark;
			lastMark = currentTime;
			return true;
		}
		else
			return false;

	}

	public long durationOfLastDelay()
	{
		return duration > 0 ? duration : 0;
	}

	public void markTime(World world)
	{
		lastMark = world.getTotalWorldTime();
	}
}
