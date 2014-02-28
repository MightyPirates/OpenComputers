/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.filler;

import buildcraft.api.core.IBox;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

public interface IFillerPattern {

	public String getUniqueTag();

	/**
	 * Creates the object that does the pattern iteration. This object may be
	 * state-full and will be used until the pattern is done or changes.
	 *
	 * @param tile the Filler
	 * @param box the area to fill
	 * @param orientation not currently used, but may be in the future (the filler needs some orientation code)
	 * @return
	 */
	public IPatternIterator createPatternIterator(TileEntity tile, IBox box, ForgeDirection orientation);

	@SideOnly(Side.CLIENT)
	public IIcon getIcon();

	public String getDisplayName();
}
