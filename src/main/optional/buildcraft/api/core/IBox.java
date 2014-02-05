/** 
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.core;

import net.minecraft.world.World;

public interface IBox {

	public void expand(int amount);

	public void contract(int amount);

	public boolean contains(int x, int y, int z);

	public Position pMin();

	public Position pMax();

	public void createLasers(World world, LaserKind kind);

	public void deleteLasers();

}
