/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.fuels;

import net.minecraft.item.ItemStack;

public class RainSubstrate {
	/**
	 * Rain substrate capable of activating the rainmaker.
	 */
	public ItemStack item;
	/**
	 * Duration of the rain shower triggered by this substrate in Minecraft ticks.
	 */
	public int duration;
	/**
	 * Speed of activation sequence triggered.
	 */
	public float speed;

	public boolean reverse;

	public RainSubstrate(ItemStack item, int duration, float speed) {
		this(item, duration, speed, false);
	}

	public RainSubstrate(ItemStack item, float speed) {
		this(item, 0, speed, true);
	}

	public RainSubstrate(ItemStack item, int duration, float speed, boolean reverse) {
		this.item = item;
		this.duration = duration;
		this.speed = speed;
		this.reverse = reverse;
	}
}
