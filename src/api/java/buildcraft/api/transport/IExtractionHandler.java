/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.transport;

import net.minecraft.world.World;

/**
 * Implement and register with the PipeManager if you want to suppress connections from wooden pipes.
 */
public interface IExtractionHandler {

	/**
	 * Can this pipe extract items from the block located at these coordinates?
	 * param extractor can be null
	 */
	boolean canExtractItems(Object extractor, World world, int i, int j, int k);

	/**
	 * Can this pipe extract liquids from the block located at these coordinates?
	 * param extractor can be null
	 */
	boolean canExtractFluids(Object extractor, World world, int i, int j, int k);
}
